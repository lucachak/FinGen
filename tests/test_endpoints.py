"""Integração das rotas /api/ia/* com o OpenRouter simulado.
Rodar com: python -m unittest discover -s tests -v
"""
import io
import json
import sys
import unittest
from pathlib import Path

import httpx
from fastapi.testclient import TestClient
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import main  # noqa: E402
from llm import OpenRouterClient  # noqa: E402


def png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (4, 4), "white").save(buf, format="PNG")
    return buf.getvalue()


class EndpointTests(unittest.TestCase):
    def setUp(self):
        self.requests = []
        self.reply = "texto"
        self.status = 200

        def handler(request):
            body = json.loads(request.content)
            self.requests.append(body)
            if self.status != 200:
                return httpx.Response(self.status, json={})
            return httpx.Response(200, json={"choices": [{"message": {"content": self.reply}}]})

        main.llm = OpenRouterClient(
            "sk-test",
            default_model="default/model",
            fallback_model="fallback/model:free",
            vision_model="vision/model:free",
            transport=httpx.MockTransport(handler),
        )
        self.client = TestClient(main.app)

    def contas(self, n=6):
        return [
            {"data": f"2026-0{i % 9 + 1}-10", "descricao": f"COMPRA {i}", "valor": 10.0 * (i + 1),
             "tipo": "SAIDA" if i else "ENTRADA", "categoria": "X", "responsavel": "A"}
            for i in range(n)
        ]

    def test_health_reports_provider(self):
        data = self.client.get("/").json()
        self.assertEqual(data["provider"], "openrouter")
        self.assertTrue(data["configured"])

    def test_analisar_uses_selected_model(self):
        self.reply = "<p>insight</p>"
        r = self.client.post("/api/ia/analisar", json={"contas": self.contas(), "model": "meta-llama/llama-3.3-70b-instruct:free"})
        self.assertEqual(r.json()["insight"], "<p>insight</p>")
        self.assertEqual(self.requests[0]["model"], "meta-llama/llama-3.3-70b-instruct:free")

    def test_analisar_without_model_uses_default(self):
        self.client.post("/api/ia/analisar", json={"contas": self.contas()})
        self.assertEqual(self.requests[0]["model"], "default/model")

    def test_analisar_reports_llm_failure_without_stack_trace(self):
        self.status = 503
        r = self.client.post("/api/ia/analisar", json={"contas": self.contas()})
        self.assertIn("indisponíveis", r.json()["insight"])

    def test_investimentos(self):
        self.reply = "<h3>plano</h3>"
        r = self.client.post("/api/ia/investimentos", json={"transacoes": self.contas(), "model": "openai/gpt-4o-mini"})
        self.assertEqual(r.json()["relatorio"], "<h3>plano</h3>")
        self.assertEqual(self.requests[0]["model"], "openai/gpt-4o-mini")

    def test_csv_extrato_categorizes_unknown_rows_with_selected_model(self):
        self.reply = json.dumps({"LOJA MISTERIOSA": "LAZER"})
        csv = "data,descricao,doc,valor\n2026-01-05,LOJA MISTERIOSA,DOC1,-50.00\n2026-01-06,IFOOD PEDIDO,DOC2,-30.00\n"
        r = self.client.post(
            "/api/ia/extrato",
            files={"file": ("e.csv", csv.encode(), "text/csv")},
            data={"model": "google/gemma-3-27b-it:free"},
        )
        body = r.json()
        self.assertEqual(body["status"], "sucesso", body)
        cats = {t["descricao"]: t["categoria"] for t in body["transacoes"]}
        self.assertEqual(cats.get("LOJA MISTERIOSA"), "LAZER")
        self.assertEqual(cats.get("IFOOD PEDIDO"), "DELIVERY")  # resolvido localmente, sem IA
        self.assertEqual(len(self.requests), 1)
        self.assertEqual(self.requests[0]["model"], "google/gemma-3-27b-it:free")
        # o Java lê _debug.categorizadas_via_ia para mostrar quantas linhas a IA categorizou
        self.assertEqual(body["_debug"]["categorizadas_via_ia"], 1)

    def test_image_extrato_goes_through_vision_model_as_image_part(self):
        self.reply = json.dumps({
            "titular": "Ana Souza",
            "transacoes": [{"data": "2026-02-01", "descricao": "MERCADO", "valor": 12.5, "tipo": "SAIDA",
                            "categoria": "SUPERMERCADO", "frequencia": "AVULSA"}],
        })
        r = self.client.post(
            "/api/ia/extrato",
            files={"file": ("foto.png", png_bytes(), "image/png")},
            data={"vision_model": "qwen/qwen2.5-vl-72b-instruct:free"},
        )
        body = r.json()
        self.assertEqual(body["status"], "sucesso", body)
        self.assertEqual(body["titular"], "Ana Souza")
        sent = self.requests[0]
        self.assertEqual(sent["model"], "qwen/qwen2.5-vl-72b-instruct:free")
        parts = sent["messages"][0]["content"]
        self.assertEqual([p["type"] for p in parts], ["text", "image_url"])
        self.assertTrue(parts[1]["image_url"]["url"].startswith("data:image/png;base64,"))

    def test_image_extrato_with_garbage_reply_is_a_clean_error(self):
        self.reply = "desculpe, não consegui"
        r = self.client.post("/api/ia/extrato", files={"file": ("foto.png", png_bytes(), "image/png")})
        body = r.json()
        self.assertEqual(body["status"], "erro")
        self.assertIn("não conseguiu ler", body["mensagem"])

    def test_unsupported_file_type_is_rejected_before_calling_the_model(self):
        r = self.client.post("/api/ia/extrato", files={"file": ("x.docx", b"123", "application/vnd.openxmlformats")})
        self.assertEqual(r.json()["status"], "erro")
        self.assertEqual(self.requests, [])

    def test_recibo(self):
        self.reply = json.dumps({"transacao": {"data": "2026-03-01", "descricao": "Farmácia", "valor": 23.9,
                                               "tipo": "SAIDA", "categoria": "FARMACIA", "frequencia": "AVULSA"}})
        r = self.client.post("/api/ia/recibo", files={"file": ("r.png", png_bytes(), "image/png")},
                             data={"vision_model": "google/gemma-3-27b-it:free"})
        body = r.json()
        self.assertEqual(body["status"], "sucesso", body)
        self.assertEqual(body["transacao"]["descricao"], "Farmácia")
        self.assertEqual(self.requests[0]["model"], "google/gemma-3-27b-it:free")


if __name__ == "__main__":
    unittest.main()
