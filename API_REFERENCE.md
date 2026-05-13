# FinGen — REST API Reference (Mobile)

Base URL: `https://fingen-app.onrender.com`  
All `/api/v1/**` endpoints (except auth) require:
```
Authorization: Bearer <jwt_token>
```

---

## Auth

| Method | Endpoint | Body | Returns |
|--------|----------|------|---------|
| `POST` | `/api/v1/auth/register` | `{ email, username, password }` | `AuthResponse` |
| `POST` | `/api/v1/auth/login` | `{ email, password }` | `AuthResponse` |
| `GET`  | `/api/v1/auth/me` | — | `AuthResponse` |

**AuthResponse**
```json
{
  "token": "eyJhbGci...",
  "userId": "uuid",
  "email": "user@email.com",
  "username": "lucas",
  "setupCompleted": true
}
```

---

## Dashboard

| Method | Endpoint | Returns |
|--------|----------|---------|
| `GET` | `/api/v1/dashboard/summary` | KPIs, FCF, patrimônio |
| `GET` | `/api/v1/dashboard/chart-data` | Doughnut + line chart data |
| `GET` | `/api/v1/dashboard/proximas-contas` | Upcoming bills |
| `GET` | `/api/v1/dashboard/transacoes-recentes` | Last 5 paid transactions |

---

## Contas (Transactions)

| Method | Endpoint | Notes |
|--------|----------|-------|
| `GET`    | `/api/v1/contas` | `?status=PENDENTE&escopo=CASA` + pagination |
| `GET`    | `/api/v1/contas/{id}` | Single transaction |
| `POST`   | `/api/v1/contas` | `multipart/form-data` — includes optional `comprovante` file |
| `PUT`    | `/api/v1/contas/{id}` | `multipart/form-data` |
| `PATCH`  | `/api/v1/contas/{id}/pagar` | Quick-pay, no body needed |
| `DELETE` | `/api/v1/contas/{id}` | — |
| `POST`   | `/api/v1/contas/lote` | Batch import `[ ContaRequest, ... ]` |

**ContaRequest**
```json
{
  "descricao": "Conta de luz",
  "valor": 180.50,
  "tipo": "DESPESA",
  "escopo": "CASA",
  "prioridade": "ALTA",
  "frequencia": "MENSAL",
  "dataVencimento": "2024-09-10",
  "categoriaId": 2,
  "responsavelId": "uuid"
}
```

---

## Metas (Goals)

| Method | Endpoint | Notes |
|--------|----------|-------|
| `GET`    | `/api/v1/metas` | All active goals |
| `GET`    | `/api/v1/metas/{id}` | — |
| `POST`   | `/api/v1/metas` | Create goal |
| `PUT`    | `/api/v1/metas/{id}` | Update goal |
| `DELETE` | `/api/v1/metas/{id}` | — |
| `POST`   | `/api/v1/metas/ai-suggest` | AI suggestions (not saved yet) |
| `POST`   | `/api/v1/metas/ai-criar` | Save an AI suggestion as a real Meta |

**Naturezas válidas:** `VIAGEM` `CARRO` `CASA` `APOSENTADORIA` `RESERVA_EMERGENCIA` `EDUCACAO` `OUTRO`

---

## Orçamentos (Budgets)

| Method | Endpoint | Notes |
|--------|----------|-------|
| `GET`    | `/api/v1/orcamentos` | With consumption % and status (NORMAL/ALERTA/CRÍTICO) |
| `POST`   | `/api/v1/orcamentos` | Create budget |
| `PUT`    | `/api/v1/orcamentos/{id}` | — |
| `DELETE` | `/api/v1/orcamentos/{id}` | — |
| `POST`   | `/api/v1/orcamentos/gerar-automatico` | Auto-generate from last 3mo average |

---

## Investimentos (Portfolio)

| Method | Endpoint | Notes |
|--------|----------|-------|
| `GET`    | `/api/v1/investimentos` | Portfolio + total ROI |
| `POST`   | `/api/v1/investimentos` | Add asset |
| `PUT`    | `/api/v1/investimentos/{id}` | — |
| `DELETE` | `/api/v1/investimentos/{id}` | — |
| `POST`   | `/api/v1/investimentos/sync` | Sync market prices |

**Tipos de ativo:** `TESOURO_DIRETO` `CDB` `LCI_LCA` `ACOES` `FIIS` `FUNDOS` `CRIPTO` `POUPANCA` `OUTROS`

---

## Transações Recorrentes

| Method | Endpoint | Notes |
|--------|----------|-------|
| `GET`    | `/api/v1/recorrentes` | All recurring rules |
| `POST`   | `/api/v1/recorrentes` | Create rule |
| `PUT`    | `/api/v1/recorrentes/{id}` | — |
| `DELETE` | `/api/v1/recorrentes/{id}` | — |

---

## IA (AI Assistant)

| Method | Endpoint | Notes |
|--------|----------|-------|
| `POST`  | `/api/v1/ia/chat` | `{ "message": "..." }` → OpenRouter chat |
| `POST`  | `/api/v1/ia/upload-extrato` | `multipart/form-data` PDF/image → Gemini staging |
| `POST`  | `/api/v1/ia/confirmar` | Confirm staged transactions |
| `GET`   | `/api/v1/ia/consultor-pessoal` | Personalized investment plan (Gemini) |
| `GET`   | `/api/v1/ia/analisar-anomalias` | Spending anomaly detection |
| `GET`   | `/api/v1/ia/status` | Health check for both AI services |

---

## Wealth

Already REST — these routes exist and are ready:

| Method | Endpoint |
|--------|----------|
| `GET`  | `/api/v1/wealth/summary` |
| `GET`  | `/api/v1/wealth/history` |
| `POST` | `/api/v1/wealth/assets` |

---

## Error Response Format

```json
{
  "status": 400,
  "error": "Validation failed",
  "timestamp": "2024-08-01T10:30:00",
  "fields": {
    "email": "must not be blank",
    "valor": "must be greater than 0"
  }
}
```

HTTP codes used: `200` `201` `204` `400` `401` `403` `404` `500`

---

## Enums Reference

```
TipoTransacao:    RECEITA | DESPESA
EscopoTransacao:  CASA | PESSOAL | NEGOCIO
StatusTransacao:  PENDENTE | PAGO | ATRASADO
Prioridade:       ALTA | MEDIA | BAIXA
Frequencia:       AVULSA | MENSAL | SEMANAL | ANUAL
```