"""Testes do cliente OpenRouter. Rodar com: python -m unittest discover -s tests -v"""
import asyncio
import json
import sys
import unittest
from pathlib import Path

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from llm import LLMError, OpenRouterClient, image_message, sanitize_model  # noqa: E402


def ok(content="olá"):
    return httpx.Response(200, json={"choices": [{"message": {"content": content}}]})


def make_client(handler, **kwargs):
    return OpenRouterClient(
        "sk-test",
        default_model="default/model",
        fallback_model="fallback/model:free",
        vision_model="vision/model:free",
        transport=httpx.MockTransport(handler),
        **kwargs,
    )


def run(coro):
    return asyncio.run(coro)


class SanitizeModelTests(unittest.TestCase):
    def test_accepts_openrouter_ids(self):
        for model in ("openrouter/free", "google/gemma-3-27b-it:free", "openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet:beta"):
            self.assertEqual(sanitize_model(model), model)

    def test_rejects_anything_else(self):
        for model in (None, "", "   ", "a b", "../../etc/passwd", "x" * 200, "model\nX-Evil: 1", "<script>"):
            self.assertIsNone(sanitize_model(model), repr(model))


class CompleteTests(unittest.TestCase):
    def test_uses_requested_model_and_sends_auth(self):
        seen = {}

        def handler(request):
            seen["body"] = json.loads(request.content)
            seen["auth"] = request.headers["authorization"]
            return ok("resposta")

        client = make_client(handler)
        text = run(client.complete([{"role": "user", "content": "oi"}], model="meta-llama/llama-3.3-70b-instruct:free"))
        self.assertEqual(text, "resposta")
        self.assertEqual(seen["body"]["model"], "meta-llama/llama-3.3-70b-instruct:free")
        self.assertEqual(seen["auth"], "Bearer sk-test")

    def test_invalid_model_falls_back_to_default(self):
        seen = {}

        def handler(request):
            seen["model"] = json.loads(request.content)["model"]
            return ok()

        run(make_client(handler).complete([{"role": "user", "content": "x"}], model="not a model"))
        self.assertEqual(seen["model"], "default/model")

    def test_rate_limit_switches_to_fallback_model(self):
        models = []

        def handler(request):
            models.append(json.loads(request.content)["model"])
            return httpx.Response(429, json={}) if len(models) == 1 else ok("reserva")

        text = run(make_client(handler).complete([{"role": "user", "content": "x"}], model="vendor/busy:free"))
        self.assertEqual(text, "reserva")
        self.assertEqual(models, ["vendor/busy:free", "fallback/model:free"])

    def test_provider_error_inside_200_is_retried(self):
        calls = []

        def handler(request):
            calls.append(1)
            if len(calls) == 1:
                return httpx.Response(200, json={"error": {"code": 503, "message": "upstream"}})
            return ok("depois")

        self.assertEqual(run(make_client(handler).complete([{"role": "user", "content": "x"}])), "depois")

    def test_bad_key_is_fatal_and_does_not_retry(self):
        calls = []

        def handler(request):
            calls.append(1)
            return httpx.Response(401, json={})

        with self.assertRaises(LLMError) as ctx:
            run(make_client(handler).complete([{"role": "user", "content": "x"}]))
        self.assertEqual(len(calls), 1)
        self.assertIn("OPENROUTER_API_KEY", str(ctx.exception))

    def test_all_models_failing_raises_friendly_error(self):
        with self.assertRaises(LLMError) as ctx:
            run(make_client(lambda r: httpx.Response(503, json={})).complete([{"role": "user", "content": "x"}]))
        self.assertIn("indisponíveis", str(ctx.exception))

    def test_json_mode_retries_without_response_format_on_400(self):
        formats = []

        def handler(request):
            body = json.loads(request.content)
            formats.append("response_format" in body)
            return httpx.Response(400, json={}) if body.get("response_format") else ok('{"a": 1}')

        text = run(make_client(handler).complete([{"role": "user", "content": "x"}], json_mode=True))
        self.assertEqual(text, '{"a": 1}')
        self.assertEqual(formats, [True, False])

    def test_vision_defaults_to_vision_model(self):
        seen = {}

        def handler(request):
            seen["model"] = json.loads(request.content)["model"]
            return ok()

        run(make_client(handler).complete([image_message("lê", b"\x89PNG", "image/png")], vision=True))
        self.assertEqual(seen["model"], "vision/model:free")

    def test_missing_key_fails_clearly_without_network(self):
        def handler(request):
            raise AssertionError("não deveria chamar a rede")

        client = OpenRouterClient("", transport=httpx.MockTransport(handler))
        with self.assertRaises(LLMError) as ctx:
            run(client.complete([{"role": "user", "content": "x"}]))
        self.assertIn("OPENROUTER_API_KEY", str(ctx.exception))

    def test_error_message_never_contains_the_key(self):
        with self.assertRaises(LLMError) as ctx:
            run(make_client(lambda r: httpx.Response(500, json={})).complete([{"role": "user", "content": "x"}]))
        self.assertNotIn("sk-test", str(ctx.exception))


class ImageMessageTests(unittest.TestCase):
    def test_builds_data_uri(self):
        msg = image_message("lê", b"abc", "image/jpeg")
        self.assertEqual(msg["role"], "user")
        self.assertEqual(msg["content"][1]["image_url"]["url"], "data:image/jpeg;base64,YWJj")


if __name__ == "__main__":
    unittest.main()
