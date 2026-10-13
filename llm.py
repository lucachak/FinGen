"""
Cliente OpenRouter do microserviço de IA do FinGen.

O OpenRouter expõe uma API compatível com a da OpenAI e dá acesso a centenas de
modelos com a mesma chave. Este módulo é o único ponto do serviço Python que
fala com um provedor de LLM.

Escolha de modelo, da mais para a menos específica:
  1. o `model` (ou `vision_model`) enviado pelo Java na requisição, que já vem
     validado contra o catálogo e escolhido pelo usuário;
  2. as variáveis AI_MODEL_DEFAULT / AI_MODEL_VISION;
  3. AI_MODEL_FALLBACK, usado quando o modelo escolhido está indisponível.
"""
import base64
import logging
import re
from typing import Any

import httpx
from decouple import config

logger = logging.getLogger("fingen.llm")

# "vendor/model" com sufixo opcional (":free", ":online"). Barra o que não é um
# id de modelo antes de ele virar parte do corpo de uma requisição paga.
_MODEL_ID = re.compile(r"^[A-Za-z0-9][\w.\-]{0,60}(/[\w.\-]{1,80})?(:[\w.\-]{1,30})?$")

# Estados em que vale tentar o modelo de reserva: limite de taxa, provedor
# fora do ar ou modelo que não existe mais.
_RETRY_STATUS = {404, 408, 429, 500, 502, 503, 504}


class LLMError(RuntimeError):
    """Falha ao obter resposta do provedor. A mensagem é segura para exibir."""


def sanitize_model(model: str | None) -> str | None:
    """Devolve o id do modelo se tiver formato válido, senão None."""
    if not model:
        return None
    model = model.strip()
    return model if _MODEL_ID.match(model) else None


def text_message(role: str, text: str) -> dict[str, Any]:
    return {"role": role, "content": text}


def image_message(prompt: str, data: bytes, mime_type: str) -> dict[str, Any]:
    """Mensagem de usuário com uma imagem embutida (data URI) e um texto."""
    encoded = base64.b64encode(data).decode("ascii")
    return {
        "role": "user",
        "content": [
            {"type": "text", "text": prompt},
            {"type": "image_url", "image_url": {"url": f"data:{mime_type};base64,{encoded}"}},
        ],
    }


class OpenRouterClient:
    def __init__(
        self,
        api_key: str,
        *,
        base_url: str = "https://openrouter.ai/api/v1",
        default_model: str = "openrouter/free",
        fallback_model: str = "google/gemma-3-27b-it:free",
        vision_model: str = "google/gemma-3-27b-it:free",
        referer: str = "http://localhost:8081",
        title: str = "FinGen",
        timeout: float = 60.0,
        transport: httpx.AsyncBaseTransport | None = None,
    ):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.default_model = default_model
        self.fallback_model = fallback_model
        self.vision_model = vision_model
        self.referer = referer
        self.title = title
        self.timeout = timeout
        self._transport = transport

    @classmethod
    def from_env(cls) -> "OpenRouterClient":
        # Chave ausente não derruba o serviço na subida: o erro aparece, claro,
        # só quando alguém tenta usar a IA.
        return cls(
            api_key=config("OPENROUTER_API_KEY", default=""),
            base_url=config("OPENROUTER_BASE_URL", default="https://openrouter.ai/api/v1"),
            default_model=config("AI_MODEL_DEFAULT", default="openrouter/free"),
            fallback_model=config("AI_MODEL_FALLBACK", default="google/gemma-3-27b-it:free"),
            vision_model=config("AI_MODEL_VISION", default="google/gemma-3-27b-it:free"),
            referer=config("BASE_URL", default="http://localhost:8081"),
        )

    @property
    def configured(self) -> bool:
        return bool(self.api_key)

    async def complete(
        self,
        messages: list[dict[str, Any]],
        *,
        model: str | None = None,
        vision: bool = False,
        json_mode: bool = False,
        temperature: float | None = None,
        max_tokens: int | None = None,
    ) -> str:
        """Envia a conversa e devolve o texto da resposta.

        `vision=True` indica que as mensagens carregam imagem; nesse caso o
        modelo padrão passa a ser o de visão.
        """
        if not self.configured:
            raise LLMError("A chave OPENROUTER_API_KEY não está configurada no serviço de IA.")

        base = self.vision_model if vision else self.default_model
        chosen = sanitize_model(model) or base
        candidates = list(dict.fromkeys([chosen, sanitize_model(self.fallback_model) or base]))
        # Modelo de texto não lê imagem: na reserva de visão usa o modelo de visão.
        if vision:
            candidates = list(dict.fromkeys([chosen, self.vision_model]))

        last_error = "sem resposta"
        for candidate in candidates:
            try:
                return await self._call(candidate, messages, json_mode, temperature, max_tokens)
            except _Retryable as exc:
                last_error = str(exc)
                logger.warning("Modelo %s indisponível (%s), tentando reserva.", candidate, exc)
            except _Fatal as exc:
                raise LLMError(str(exc)) from None

        raise LLMError(f"Os modelos de IA estão indisponíveis no momento ({last_error}). Tente novamente.")

    async def _call(
        self,
        model: str,
        messages: list[dict[str, Any]],
        json_mode: bool,
        temperature: float | None,
        max_tokens: int | None,
    ) -> str:
        payload: dict[str, Any] = {"model": model, "messages": messages}
        if temperature is not None:
            payload["temperature"] = temperature
        if max_tokens is not None:
            payload["max_tokens"] = max_tokens
        if json_mode:
            payload["response_format"] = {"type": "json_object"}

        status, body = await self._post(payload)

        # Nem todo modelo aceita response_format. O prompt já pede JSON, então
        # repete sem o parâmetro em vez de trocar de modelo.
        if status == 400 and json_mode:
            payload.pop("response_format")
            status, body = await self._post(payload)

        if status == 401:
            raise _Fatal("A chave do OpenRouter foi recusada. Verifique OPENROUTER_API_KEY.")
        if status == 402:
            raise _Fatal(f"O modelo {model} exige créditos na conta do OpenRouter.")
        if status in _RETRY_STATUS:
            raise _Retryable(f"HTTP {status}")
        if status != 200:
            raise _Fatal(f"O OpenRouter respondeu HTTP {status}.")

        try:
            content = body["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError):
            # O OpenRouter às vezes devolve 200 com {"error": {...}} do provedor.
            error = body.get("error") if isinstance(body, dict) else None
            code = error.get("code") if isinstance(error, dict) else None
            if code in _RETRY_STATUS:
                raise _Retryable(f"provedor HTTP {code}") from None
            raise _Retryable("resposta vazia") from None

        if isinstance(content, list):  # partes de conteúdo
            content = "".join(p.get("text", "") for p in content if isinstance(p, dict))
        if not content or not str(content).strip():
            raise _Retryable("resposta vazia")
        return str(content)

    async def _post(self, payload: dict[str, Any]) -> tuple[int, Any]:
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": self.referer,
            "X-Title": self.title,
        }
        try:
            async with httpx.AsyncClient(timeout=self.timeout, transport=self._transport) as http:
                response = await http.post(f"{self.base_url}/chat/completions", json=payload, headers=headers)
        except httpx.TimeoutException:
            raise _Retryable("tempo esgotado") from None
        except httpx.HTTPError as exc:
            raise _Retryable(f"falha de rede ({type(exc).__name__})") from None

        try:
            return response.status_code, response.json()
        except ValueError:
            return response.status_code, None


class _Retryable(Exception):
    """Vale tentar o modelo de reserva."""


class _Fatal(Exception):
    """Trocar de modelo não ajuda (chave inválida, sem créditos...)."""
