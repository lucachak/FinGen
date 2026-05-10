import io
import json
import logging
import re
import time
from datetime import datetime

import numpy as np
import pandas as pd
import yfinance as yf
from decouple import config
from fastapi import FastAPI, File, Form, Request, UploadFile
from starlette.concurrency import run_in_threadpool
from google import genai
from google.genai import types
from PIL import Image
from pypdf import PdfReader
from sklearn.ensemble import IsolationForest
from sklearn.linear_model import LinearRegression
from pydantic import BaseModel, Field
from enum import Enum

'''

    ██████╗ ██████╗ ██████╗ ██████╗ ██╗   ██╗███████╗
    ██╔══██╗██╔══██╗██╔══██╗██╔══██╗██║   ██║██╔════╝
    ██████╔╝██████╔╝██████╔╝██████╔╝██║   ██║█████╗
    ██╔═══╝ ██╔══██╗██╔══██╗██╔══██╗██║   ██║██╔══╝
    ██║     ██║  ██║██║  ██║██║  ██║╚██████╔╝███████╗
    ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝

    ██████╗ ██████╗ ██████╗ ██████╗ ██╗   ██╗███████╗
    ██╔══██╗██╔══██╗██╔══██╗██╔══██╗██║   ██║██╔════╝
    ██████╔╝██████╔╝██████╔╝██████╔╝██║   ██║█████╗
    ██╔═══╝ ██╔══██╗██╔══██╗██╔══██╗██║   ██║██╔══╝
    ██║     ██║  ██║██║  ██║██║  ██║╚██████╔╝███████╗
    ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝


    simple api point for financial analysis
    in order to help people to user and make the best
    out of their financial data and money 
    best possible investments, and ways to save money
    along side best way to refinance the debts 
'''


# =========================================================
# 🪵 LOGGING DE DEBUG
# =========================================================
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("financas_ia")

class TransacaoDTO(BaseModel):
    data: str = ""
    descricao: str = ""
    valor: float = 0.0
    tipo: str = "SAIDA"
    categoria: str = "Sem Categoria"
    responsavel: str = "Desconhecido"
    documento: str = ""
    frequencia: str = "AVULSA"

class PerfilFinanceiro(BaseModel):
    tipo: str = "CONSERVADOR"
    meta_poupanca: float = 20.0
    teto_essenciais: float = 50.0
    orcamento_mensal: float = 3500.0

class AnaliseRequest(BaseModel):
    contas: list[TransacaoDTO]
    perfil: PerfilFinanceiro = Field(default_factory=PerfilFinanceiro)

class InvestimentoRequest(BaseModel):
    transacoes: list[TransacaoDTO]
    perfil: PerfilFinanceiro = Field(default_factory=PerfilFinanceiro)

class Frequencia(str, Enum):
    AVULSA = "AVULSA"
    MENSAL = "MENSAL"
    SEMANAL = "SEMANAL"
    ANUAL = "ANUAL"

class ExtratoTransacaoItem(BaseModel):
    data: str = Field(description="Data da transação (YYYY-MM-DD)")
    descricao: str = Field(description="Nome do serviço, loja ou transferência")
    valor: float = Field(description="Valor numérico obrigatoriamente absoluto (positivo)")
    tipo: str = Field(description="SAIDA ou ENTRADA")
    categoria: str = Field(description="MORADIA, SUPERMERCADO, DELIVERY, TRANSPORTE, FARMACIA, LAZER, SALARIO, TRANSFERENCIA_ENTRADA, REEMBOLSO ou OUTROS")
    frequencia: Frequencia = Field(description="Padrão de repetição detectado (MENSAL, SEMANAL, ANUAL). Se for algo pontual, use AVULSA.", default=Frequencia.AVULSA)

class ExtratoResponseSchema(BaseModel):
    titular: str = Field(description="Nome encontrado do titular do extrato. Desconhecido se não houver.")
    transacoes: list[ExtratoTransacaoItem]
    
class ExtratoAPIResponse(BaseModel):
    status: str
    titular: str
    transacoes: list[dict]
    _debug: dict = {}
    mensagem: str | None = None

app = FastAPI()
client = genai.Client(api_key=config("GEMINI_TOKEN"))

# =========================================================
# 🧪 MODO DE TESTE (MOCK) - ALTERE PARA FALSE PARA USAR A IA REAL
# =========================================================
MOCK_API_GEMINI = False


# =========================================================
# 🛠️ UTILITÁRIOS
# =========================================================

def extrair_json_seguro(texto_ia: str):
    """Isola rigorosamente o bloco JSON da resposta da IA."""
    texto_limpo = texto_ia.replace("```json", "").replace("```", "").strip()
    try:
        return json.loads(texto_limpo)
    except Exception:
        pass

    inicio_array = texto_limpo.find("[")
    fim_array    = texto_limpo.rfind("]")
    inicio_obj   = texto_limpo.find("{")
    fim_obj      = texto_limpo.rfind("}")

    try:
        if (
            inicio_array != -1
            and fim_array != -1
            and (inicio_obj == -1 or inicio_array < inicio_obj)
        ):
            return json.loads(texto_limpo[inicio_array : fim_array + 1])
        elif inicio_obj != -1 and fim_obj != -1:
            return json.loads(texto_limpo[inicio_obj : fim_obj + 1])
    except Exception:
        pass

    raise ValueError("A IA foi tagarela demais e não gerou um JSON legível.")


# =========================================================
# 📚 DICIONÁRIO BASE DE CATEGORIAS
# =========================================================
DICIONARIO_BASE = {
    # Delivery
    "IFOOD": "DELIVERY",
    # Transporte
    "UBER": "TRANSPORTE",
    "POSTO SHELL": "TRANSPORTE",
    "COMBUSTIVEL": "TRANSPORTE",
    # Moradia / utilidades
    "SABESP": "MORADIA",
    "ELEKTRO": "MORADIA",
    "ENERGIA": "MORADIA",
    "TARIFA MANUTENCAO": "MORADIA",
    "TARIFA TED": "MORADIA",
    # Farmácia
    "DROGASIL": "FARMACIA",
    "FARMACIA": "FARMACIA",
    # Lazer / compras online
    "AMAZON": "LAZER",
    "ALIEXPRESS": "LAZER",
    "MERCADO LIVRE": "LAZER",
    "MAGAZINE LUIZA": "LAZER",
    "NETFLIX": "LAZER",
    # Supermercado / alimentação
    "PADARIA": "SUPERMERCADO",
    "SUPERMERCADO": "SUPERMERCADO",
    # Entradas
    "SALARIO": "SALARIO",
    "TED RECEBIDA": "TRANSFERENCIA_ENTRADA",
    "PIX RECEBIDO": "TRANSFERENCIA_ENTRADA",
    "TRANSFERENCIA RECEBIDA": "TRANSFERENCIA_ENTRADA",
    "REEMBOLSO": "REEMBOLSO",
    "DEVOLUCAO": "REEMBOLSO",
}


def categorizar_localmente(descricao: str) -> str | None:
    """Tenta categorizar a descrição usando o dicionário local. Retorna None se não encontrar."""
    desc_upper = descricao.upper()
    for chave, categoria in DICIONARIO_BASE.items():
        if chave in desc_upper:
            return categoria
    return None


def determinar_tipo(descricao: str, valor_numerico: float) -> str:
    """Determina se a transação é ENTRADA ou SAIDA."""
    desc_upper = descricao.upper()
    if any(p in desc_upper for p in [
        "RECEBIDO", "DEVOLUCAO", "SALARIO", "PIX RECEBIDO",
        "REEMBOLSO", "TED RECEBIDA", "TRANSFERENCIA RECEBIDA",
    ]):
        return "ENTRADA"
    if any(p in desc_upper for p in [
        "COMPRA", "IFOOD", "TARIFA", "PAGAMENTO", "UBER", "POSTO",
    ]):
        return "SAIDA"
    return "ENTRADA" if valor_numerico >= 0 else "SAIDA"


# =========================================================
# 📄 PARSER DE PDF — CIENTE DA ESTRUTURA DE COLUNAS
# =========================================================

DATE_RE  = re.compile(r"^\d{2}/\d{2}/\d{4}$")
VALUE_RE = re.compile(r"^-?\d{1,3}(?:\.\d{3})*,\d{2}$")

LINHAS_CABECALHO = {
    "data", "histórico", "historico", "documento",
    "valor (r$)", "saldo (r$)",
}


def parsear_pdf_tabular(conteudo_bytes: bytes) -> tuple[str, list[dict]]:
    """
    Lê o PDF coluna a coluna.

    O pypdf extrai tabelas com colunas em linhas separadas:
        linha 0: data        (dd/mm/yyyy)
        linha 1: histórico   (descrição da transação)
        linha 2: documento   (código interno)
        linha 3: valor       (-1.234,56 ou 1.234,56)
        linha 4: saldo       (1.234,56)

    Retorna (titular, lista_de_transacoes_brutas).
    """
    t0 = time.perf_counter()
    reader = PdfReader(io.BytesIO(conteudo_bytes))
    n_paginas = len(reader.pages)
    logger.debug("📄 PDF aberto: %d páginas", n_paginas)

    texto_completo = ""
    for i, pagina in enumerate(reader.pages):
        texto_pagina = pagina.extract_text() or ""
        logger.debug("  Página %d/%d: %d chars extraídos", i + 1, n_paginas, len(texto_pagina))
        texto_completo += texto_pagina + "\n"

    # ── Extração do titular ──────────────────────────────────────────────────
    titular = "Desconhecido"
    match_tit = re.search(
        r"(?i)titular\s*\n?\s*([A-Za-zÀ-ÿ\s]+?)(?:\n|cpf|período|documento|$)",
        texto_completo[:3000],
    )
    if match_tit:
        titular = match_tit.group(1).strip()
        logger.debug("🪪 Titular detectado: '%s'", titular)
    else:
        logger.warning("⚠️ Titular não encontrado no cabeçalho do PDF.")

    # ── Limpeza e tokenização linha a linha ──────────────────────────────────
    linhas = [l.strip() for l in texto_completo.split("\n")]
    linhas = [
        l for l in linhas
        if l and l.lower() not in LINHAS_CABECALHO
    ]

    logger.debug("🔍 Total de linhas após limpeza: %d", len(linhas))

    # ── Parsing: blocos de 5 linhas (data, hist, doc, valor, saldo) ──────────
    transacoes: list[dict] = []
    i = 0
    skipped = 0

    while i < len(linhas):
        linha_atual = linhas[i]

        if not DATE_RE.match(linha_atual):
            i += 1
            skipped += 1
            continue

        # Verifica se há linhas suficientes e se valor/saldo têm formato correto
        if i + 4 >= len(linhas):
            logger.debug("  Linha %d: data encontrada mas não há 4 linhas seguintes — fim do arquivo.", i)
            i += 1
            continue

        data_str  = linhas[i]
        historico = linhas[i + 1]
        documento = linhas[i + 2]
        valor_str = linhas[i + 3]
        saldo_str = linhas[i + 4]

        if not (VALUE_RE.match(valor_str) and VALUE_RE.match(saldo_str)):
            logger.debug(
                "  Linha %d: data '%s' mas valor='%s' / saldo='%s' não batem — pulando.",
                i, data_str, valor_str, saldo_str,
            )
            i += 1
            skipped += 1
            continue

        # Converte valor para float (formato BR: "1.234,56" → 1234.56)
        v_limpo = valor_str.replace(".", "").replace(",", ".")
        try:
            valor_numerico = float(v_limpo)
        except ValueError:
            logger.warning("  ⚠️ Valor inválido na linha %d: '%s'", i + 3, valor_str)
            i += 1
            continue

        # Formata data para YYYY-MM-DD
        try:
            d, m, y = data_str.split("/")
            data_formatada = f"{y}-{m}-{d}"
        except ValueError:
            data_formatada = datetime.now().strftime("%Y-%m-%d")

        tipo = determinar_tipo(historico, valor_numerico)

        transacoes.append({
            "data":      data_formatada,
            "descricao": historico,
            "documento": documento,
            "valor":     abs(valor_numerico),
            "tipo":      tipo,
            # 'categoria' será preenchida adiante
        })

        i += 5  # avança o bloco inteiro

    elapsed = time.perf_counter() - t0
    logger.info(
        "✅ PDF parseado: %d transações extraídas / %d linhas ignoradas (%.2fs)",
        len(transacoes), skipped, elapsed,
    )
    return titular, transacoes


async def categorizar_em_lote_via_ia(descricoes: list[str]) -> dict[str, str]:
    """
    Envia descrições desconhecidas para a IA categorizar em lote.
    Retorna {descricao: categoria}.
    """
    if not descricoes:
        return {}

    descricoes_unicas = list(dict.fromkeys(descricoes))  # preserva ordem, sem duplicatas
    logger.debug("🤖 Enviando %d descrições únicas para categorização IA...", len(descricoes_unicas))

    prompt = (
        "Você é um categorizador financeiro. Para cada item abaixo, retorne UM JSON puro "
        "(sem markdown, sem explicações) no formato {\"DESCRICAO\": \"CATEGORIA\"}.\n"
        "Categorias permitidas: MORADIA, SUPERMERCADO, DELIVERY, TRANSPORTE, FARMACIA, "
        "LAZER, SALARIO, TRANSFERENCIA_ENTRADA, REEMBOLSO, OUTROS.\n\n"
        f"Itens: {json.dumps(descricoes_unicas, ensure_ascii=False)}"
    )

    t0 = time.perf_counter()
    resposta = await client.aio.models.generate_content(
        model="gemini-2.5-flash", 
        contents=prompt,
        config=types.GenerateContentConfig(response_mime_type="application/json")
    )
    elapsed = time.perf_counter() - t0
    logger.debug("  IA respondeu em %.2fs: %s", elapsed, resposta.text[:200])

    try:
        mapa = json.loads(resposta.text)
        logger.info("🏷️  IA categorizou %d itens com sucesso.", len(mapa))
        return mapa
    except Exception as e:
        logger.error("❌ Falha ao parsear JSON da IA de categorização: %s", e)
        return {}





@app.get("/")
async def root():
    return {"message": "Api is Running..." }


@app.post("/api/ia/analisar")
async def analisar_financas(request_data: AnaliseRequest):
    logger.info("📥 POST /api/ia/analisar")
    try:
        contas_brutas = [t.model_dump() for t in request_data.contas]
        logger.debug("  Transações recebidas: %d", len(contas_brutas))

        if len(contas_brutas) < 5:
            return {"insight": "<i>Precisamos de pelo menos 5 transações para a IA encontrar padrões estatísticos.</i>"}

        def ml_sync_task(raw_data, perfil_data: PerfilFinanceiro):
            df_sync = pd.DataFrame(raw_data)
            df_sync["data"]  = pd.to_datetime(df_sync["data"])
            df_sync["valor"] = pd.to_numeric(df_sync["valor"])

            total_entrada = df_sync[df_sync["tipo"] == "ENTRADA"]["valor"].sum()
            total_saida   = df_sync[df_sync["tipo"] != "ENTRADA"]["valor"].sum()
            
            # 1. Leak Detection (Isolation Forest for Anomaly + Frequency Analysis)
            # "Bad Manners": Small transactions that happen very frequently
            modelo_ml = IsolationForest(contamination=0.1, random_state=42)
            df_sync["is_anomalia"] = modelo_ml.fit_predict(df_sync[["valor"]])
            
            leaks = []
            df_anoms = df_sync[df_sync["is_anomalia"] == -1]
            for _, row in df_anoms.iterrows():
                leaks.append(f"<b>Anomalia Detectada:</b> {row['descricao']} (R$ {row['valor']:.2f})")

            vc = df_sync[df_sync["tipo"] == "SAIDA"]["descricao"].value_counts()
            frequentes = vc[vc > 3].index.tolist()
            if frequentes:
                leaks.append(f"<b>Vazamento de Hábito:</b> Detectamos gastos repetitivos em '{', '.join(frequentes[:2])}'.")

            saldo_atual = total_entrada - total_saida
            pct_poupanca_real = (saldo_atual / total_entrada * 100) if total_entrada > 0 else 0
            objetivo_poupanca = perfil_data.meta_poupanca
            
            growth_advice = ""
            if pct_poupanca_real >= objetivo_poupanca:
                growth_advice = f"🚀 <b>Excelente!</b> Estás a poupar {pct_poupanca_real:.1f}%, superando a tua meta de {objetivo_poupanca}%. Sugerimos investir o excedente de R$ {saldo_atual:.2f}."
            else:
                deficit_pct = objetivo_poupanca - pct_poupanca_real
                growth_advice = f"⚠️ <b>Atenção:</b> Estás a poupar apenas {pct_poupanca_real:.1f}%. Precisas de cortar R$ {(deficit_pct/100 * total_entrada):.2f} adicionais para atingir a tua meta de {objetivo_poupanca}%."

            return total_saida, total_entrada, leaks, growth_advice

        total_gasto, total_receita, lista_leaks, conselho_crescimento = await run_in_threadpool(ml_sync_task, contas_brutas, request_data.perfil)
        
        if MOCK_API_GEMINI:
            return {"insight": f"<h3>ML Advisor</h3><p>{conselho_crescimento}</p><ul>{''.join([f'<li>{l}</li>' for l in lista_leaks])}</ul>"}

        prompt = (
            f"Aja como Consultor Financeiro de Elite. Perfil: {request_data.perfil.tipo}. "
            f"Receita: R$ {total_receita:.2f}, Gastos: R$ {total_gasto:.2f}.\n"
            f"Insights de ML: {lista_leaks}\n"
            f"Conselho de Crescimento: {conselho_crescimento}\n"
            "Gere uma resposta curta (HTML) motivadora focada em eliminar os vazamentos citados e CRESCER o património."
        )
        resposta = await client.aio.models.generate_content(model="gemini-2.5-flash", contents=prompt)
        return {"insight": resposta.text}

    except Exception as e:
        logger.exception("❌ Erro em /api/ia/analisar")
        return {"insight": f"<i>Erro ao processar: {str(e)}</i>"}


@app.post("/api/ia/extrato", response_model=ExtratoAPIResponse)
async def processar_extrato(
    file: UploadFile = File(...),
    historico: str   = Form(default="{}"),
):
    logger.info("📥 POST /api/ia/extrato — arquivo: '%s'", file.filename)
    t_inicio = time.perf_counter()

    try:
        # ── Dicionário dinâmico: base + histórico do usuário ─────────────────
        dic_dinamico = dict(DICIONARIO_BASE)
        try:
            hist_user = extrair_json_seguro(historico)
            dic_dinamico.update(hist_user)
            logger.debug("  Dicionário do usuário: %d entradas extras", len(hist_user))
        except Exception:
            logger.debug("  Nenhum histórico de usuário válido.")

        def categorizar_localmente_dinamico(descricao: str) -> str | None:
            desc_upper = descricao.upper()
            for chave, categoria in dic_dinamico.items():
                if chave.upper() in desc_upper:
                    return categoria
            return None

        conteudo      = await file.read()
        nome_ficheiro = (file.filename or "").lower()
        
        mime_type = file.content_type
        if not mime_type:
            if nome_ficheiro.endswith(".pdf"): mime_type = "application/pdf"
            elif nome_ficheiro.endswith(".csv"): mime_type = "text/csv"
            elif nome_ficheiro.endswith((".png", ".jpg", ".jpeg")): mime_type = f"image/{nome_ficheiro.split('.')[-1].replace('jpg', 'jpeg')}"
            else: mime_type = "text/plain"

        if mime_type == "text/csv" or nome_ficheiro.endswith(".csv"):
            logger.info("  Modo: CSV (Parsing Assíncrono Threadpool)")
            texto_extraido = conteudo.decode("utf-8", errors="replace")
            transacoes_brutas = await run_in_threadpool(_parsear_csv, texto_extraido)
            titular_extraido = await run_in_threadpool(_extrair_titular_texto, texto_extraido)
            
            transacoes_finais = []
            descricoes_desconhecidas = []
            for t in transacoes_brutas:
                desc = t.get("descricao", "")
                cat  = categorizar_localmente_dinamico(desc)
                if cat:
                    t["categoria"] = cat
                    transacoes_finais.append(t)
                else:
                    t["categoria"] = "UNKNOWN"
                    descricoes_desconhecidas.append(desc)
                    transacoes_finais.append(t)
                    
            if descricoes_desconhecidas and not MOCK_API_GEMINI:
                mapa_ia = await categorizar_em_lote_via_ia(descricoes_desconhecidas)
                for t in transacoes_finais:
                    if t.get("categoria") == "UNKNOWN":
                        t["categoria"] = mapa_ia.get(t.get("descricao", ""), "OUTROS")
                        
        elif mime_type == "application/pdf" or nome_ficheiro.endswith(".pdf"):
            logger.info("  Modo: PDF Local (Economia Massiva de Tokens via PyPDF)")
            titular_extraido, transacoes_brutas = await run_in_threadpool(parsear_pdf_tabular, conteudo)
            
            transacoes_finais = []
            descricoes_desconhecidas = []
            for t in transacoes_brutas:
                desc = t.get("descricao", "")
                cat  = categorizar_localmente_dinamico(desc)
                if cat:
                    t["categoria"] = cat
                    transacoes_finais.append(t)
                else:
                    t["categoria"] = "UNKNOWN"
                    descricoes_desconhecidas.append(desc)
                    transacoes_finais.append(t)
                    
            if descricoes_desconhecidas and not MOCK_API_GEMINI:
                mapa_ia = await categorizar_em_lote_via_ia(descricoes_desconhecidas)
                for t in transacoes_finais:
                    if t.get("categoria") == "UNKNOWN":
                        t["categoria"] = mapa_ia.get(t.get("descricao", ""), "OUTROS")
                        
        else:
            logger.info("  Modo Multimodal (Imagem OCR): Enviando foto estruturada nativamente para o Gemini.")
            prompt_extracao = (
                "Você é o poderoso bot financeiro. Analise rigorosamente os dados deste extrato (fotografia ou PDF). "
                "Extraia a lista de transações com perfeição. Considere que os valores de saída (SAIDA) "
                "devem ser sempre retornados absolutizados e o sinal interpretado como 'SAIDA'. "
                "Ignore anúncios ou saldos diários intermédios, foque nas transações puras."
            )
            
            resposta = await client.aio.models.generate_content(
                model="gemini-2.5-flash",
                contents=[
                    types.Part.from_bytes(data=conteudo, mime_type=mime_type),
                    prompt_extracao
                ],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=ExtratoResponseSchema,
                    temperature=0.0
                )
            )
            
            dados_str = resposta.text
            try:
                dados_ia = json.loads(dados_str)
                titular_extraido = dados_ia.get("titular", "Desconhecido")
                transacoes_finais = dados_ia.get("transacoes", [])
                logger.info("  IA nativamente parseou %d transações estruturadas", len(transacoes_finais))
            except Exception as jE:
                logger.error("Falha ao usar JSON gerado estruturado: %s", jE)
                transacoes_finais = []
                titular_extraido = "Desconhecido"
            
            descricoes_desconhecidas = []

        elapsed = time.perf_counter() - t_inicio
        logger.info(
            "✅ /api/ia/extrato concluído: titular='%s', %d transações em %.2fs",
            titular_extraido, len(transacoes_finais), elapsed,
        )

        return {
            "status":     "sucesso",
            "titular":    titular_extraido,
            "transacoes": transacoes_finais,
            # ── campos de debug ──────────────────────────────────────────────
            "_debug": {
                "total_brutas":             len(transacoes_brutas),
                "categorizadas_localmente": len(transacoes_finais) - len(descricoes_desconhecidas),
                "categorizadas_via_ia":     len(descricoes_desconhecidas),
                "tempo_total_segundos":     round(elapsed, 3),
            },
        }

    except Exception as e:
        logger.exception("❌ Erro em /api/ia/extrato")
        return {"status": "erro", "mensagem": str(e)}


# =========================================================
# 🔧 HELPERS LEGADOS (usados pelo fallback de CSV)
# =========================================================

def _extrair_titular_texto(texto: str) -> str:
    match = re.search(
        r"(?i)titular[\s,:-]+([A-Za-zÀ-ÿ\s]+?)(?:cpf|período|documento|\n|$)",
        texto[:2000],
    )
    if match:
        return match.group(1).split(",")[0].strip()
    return "Desconhecido"


def _parsear_csv(texto: str) -> list[dict]:
    """Parser legado de regex para CSV e formatos de texto genéricos."""
    texto_limpo = texto.replace('"', "").replace("\r", "")
    texto_limpo = re.sub(r"\n\s*,", ",", texto_limpo)

    padroes = [
        re.compile(
            r"(\d{2}[-/]\d{2}[-/]\d{4}|\d{4}[-/]\d{2}[-/]\d{2})[\s,]+(.+?)[\s,]+"
            r"(?:[A-Za-z0-9\-]+\s*[,|;]?\s*)?(-?(?:R\$)?\s*\d{1,3}(?:\.\d{3})*,\d{2}|-?\d+\.\d{2})"
        ),
        re.compile(
            r"(\d{2}\s+[A-Za-z]{3})[\s,]+(.+?)[\s,]+"
            r"(?:[A-Za-z0-9\-]+\s*[,|;]?\s*)?(-?(?:R\$)?\s*\d{1,3}(?:\.\d{3})*,\d{2}|-?\d+\.\d{2})"
        ),
        re.compile(
            r"(\d{2}/\d{2})[\s,]+(.+?)[\s,]+"
            r"(?:[A-Za-z0-9\-]+\s*[,|;]?\s*)?(-?(?:R\$)?\s*\d{1,3}(?:\.\d{3})*,\d{2})"
        ),
    ]

    hoje = datetime.now()
    transacoes: list[dict] = []

    for linha in texto_limpo.split("\n"):
        for padrao in padroes:
            match = padrao.search(linha.strip())
            if match:
                data_str, desc, valor_str = match.groups()
                v_limpo = valor_str.replace("R$", "").replace(".", "").replace(",", ".").strip()
                try:
                    valor_numerico = float(v_limpo)
                except ValueError:
                    continue

                tipo = determinar_tipo(desc, valor_numerico)

                data_formatada = f"{hoje.year}-{hoje.month:02d}-01"
                if "/" in data_str and len(data_str) == 10:
                    d, m, y = data_str.split("/")
                    data_formatada = f"{y}-{m}-{d}"
                elif "/" in data_str and len(data_str) == 5:
                    d, m = data_str.split("/")
                    data_formatada = f"{hoje.year}-{m}-{d}"

                transacoes.append({
                    "data":      data_formatada,
                    "descricao": desc.strip(),
                    "valor":     abs(valor_numerico),
                    "tipo":      tipo,
                })
                break

    return transacoes


# =========================================================
# ROTA 3: PREVISÃO DE GASTOS (Oráculo Financeiro via ML)
# =========================================================
@app.post("/api/ia/prever")
async def prever_gastos_futuros(request_data: AnaliseRequest):
    logger.info("📥 POST /api/ia/prever")
    try:
        contas = [t.model_dump() for t in request_data.contas if "SAIDA" in t.tipo.upper() or t.tipo == "SAIDA"]
        if len(contas) < 5:
            return {"mensagem": "Dados insuficientes para prever gastos. Registre mais despesas."}
        
        def previsao_sync_task(ct_list):
            df_s = pd.DataFrame(ct_list)
            df_s["data"] = pd.to_datetime(df_s["data"], errors="coerce")
            df_s = df_s.dropna(subset=["data"])
            
            df_s["mes"] = df_s["data"].dt.to_period("M")
            m_s = df_s.groupby("mes")["valor"].sum().reset_index()
            
            if len(m_s) < 2:
                return False, "Precisamos de pelo menos 2 meses de histórico de despesas para o oráculo funcionar."
                
            m_s["mes_num"] = np.arange(len(m_s))
            X_s = m_s[["mes_num"]]
            y_s = m_s["valor"]
            
            mod = LinearRegression()
            mod.fit(X_s, y_s)
            
            prox_m = [[len(m_s)]]
            prev = mod.predict(prox_m)[0]
            tend = "aumentar" if mod.coef_[0] > 0 else "diminuir"
            coef = abs(mod.coef_[0])
            return True, (prev, tend, coef)

        sucesso, result = await run_in_threadpool(previsao_sync_task, contas)
        if not sucesso:
            return {"mensagem": result}
            
        previsao, tendencia, coef_val = result
        
        html = f"<div style='padding:15px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px;'><h4 style='margin-top:0; color:#0f172a; margin-bottom: 8px;'>🔮 Oráculo Financeiro</h4><p style='color:#334155; font-size:14px; margin-bottom: 4px;'>Com base no histórico analisado, a sua projeção de gastos para o próximo mês é:</p><h2 style='color:#09090b; margin:8px 0; font-family: var(--mono, monospace);'>R$ {previsao:.2f}</h2><p style='font-size:13px; color:#64748b; margin-bottom:0;'>A sua tendência de gastos está a <strong>{tendencia}</strong> (em média R$ {coef_val:.2f} por mês).</p></div>"
        return {"insight": html}
    except Exception as e:
        logger.exception("❌ Erro em /api/ia/prever")
        return {"insight": f"<div style='color: #e11d48;'>Erro na Previsão: {str(e)}</div>"}


# =========================================================
# ROTA 4: LEITURA DE RECIBO (Multimodal Ocular)
# =========================================================
@app.post("/api/ia/recibo")
async def ler_recibo(file: UploadFile = File(...)):
    logger.info("📥 POST /api/ia/recibo")
    try:
        conteudo = await file.read()
        image = Image.open(io.BytesIO(conteudo))
        
        prompt = (
            "Analise este recibo/fatura. Extraia os dados num JSON rigoroso com a estrutura:\n"
            "{\n"
            '  "transacao": {"data": "YYYY-MM-DD", "descricao": "Nome do local de serviço", "valor": 99.99, "tipo": "SAIDA", "categoria": "MORADIA ou SUPERMERCADO ou DELIVERY ou TRANSPORTE ou FARMACIA ou LAZER ou OUTROS", "frequencia": "AVULSA"}\n'
            "}"
        )
        
        resposta = await client.aio.models.generate_content(
            model="gemini-2.5-flash",
            contents=[image, prompt],
            config=types.GenerateContentConfig(response_mime_type="application/json")
        )
        
        dados_ia = json.loads(resposta.text)
        return {"status": "sucesso", "transacao": dados_ia.get("transacao", {})}
    except Exception as e:
        logger.exception("❌ Erro em /api/ia/recibo")
        return {"status": "erro", "mensagem": str(e)}


# =========================================================
# ROTA 5: CONSULTOR DE INVESTIMENTOS
# =========================================================
@app.post("/api/ia/investimentos")
async def consultor_investimentos(request_data: InvestimentoRequest):
    logger.info("📥 POST /api/ia/investimentos")
    try:
        transacoes = [t.model_dump() for t in request_data.transacoes]

        total_receitas = sum(float(t.get("valor", 0)) for t in transacoes if t.get("tipo") == "ENTRADA")
        total_despesas = sum(float(t.get("valor", 0)) for t in transacoes if t.get("tipo") != "ENTRADA")
        sobra_caixa    = total_receitas - total_despesas
        logger.debug("  Receitas: R$ %.2f | Despesas: R$ %.2f | FCF: R$ %.2f", total_receitas, total_despesas, sobra_caixa)

        prompt = (
            f"Considere os dados privados: Receitas: R$ {total_receitas:.2f}, "
            f"Despesas: R$ {total_despesas:.2f}, FCF: R$ {sobra_caixa:.2f}.\n"
            "Escreva um relatório de aconselhamento em HTML com estratégia de alocação de risco."
        )
        resposta = await client.aio.models.generate_content(model="gemini-2.5-flash", contents=prompt)
        return {"relatorio": resposta.text}

    except Exception as e:
        logger.exception("❌ Erro em /api/ia/investimentos")
        return {"relatorio": f"<div style='color: #ef4444;'>Erro: {str(e)}</div>"}


# =========================================================
# ROTA 6: COTAÇÕES DE MERCADO (yfinance)
# =========================================================
@app.post("/api/market/quotes")
async def obter_cotacoes(request: Request):
    logger.info("📥 POST /api/market/quotes")
    try:
        dados = await request.json()
        tickers = dados.get("tickers", [])
        
        if not tickers:
            return {"status": "erro", "mensagem": "Nenhum ticker fornecido."}
            
        logger.debug("Buscando cotações para: %s", tickers)
        
        resultados = {}
        for t in tickers:
            try:
                ativo = yf.Ticker(t)
                hist = ativo.history(period="2d")
                if not hist.empty:
                    preco = float(hist["Close"].iloc[-1])
                    
                    variacao_dia = 0.0
                    variacao_pct = 0.0
                    if len(hist) > 1:
                        fechamento_anterior = float(hist["Close"].iloc[-2])
                        variacao_dia = preco - fechamento_anterior
                        variacao_pct = (variacao_dia / fechamento_anterior) * 100
                    elif len(hist) == 1:
                        abertura = float(hist["Open"].iloc[0])
                        variacao_dia = preco - abertura
                        variacao_pct = (variacao_dia / abertura) * 100 if abertura != 0 else 0
                    
                    resultados[t] = {
                        "preco_atual": round(preco, 4),
                        "variacao_dia": round(variacao_dia, 4),
                        "variacao_pct": round(variacao_pct, 4)
                    }
                else:
                    logger.warning("Histórico vazio ou não encontrado para %s", t)
            except Exception as e:
                logger.warning("Erro ao buscar cotação para %s: %s", t, e)
                
        return {"status": "sucesso", "cotacoes": resultados}
    except Exception as e:
        logger.exception("❌ Erro em /api/market/quotes")
        return {"status": "erro", "mensagem": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

