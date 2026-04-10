# Project Plan: Token Optimizer & ML Financial Advisor

## 🎯 Objective
Aggressively optimize AI token usage by moving tabular extraction (PDF/CSV) to local Python libraries, reserving Gemini exclusively for complex tasks (Image OCR and batch categorization). Additionally, implement a robust Machine Learning engine that actively analyzes "bad manners" (spending leaks) and provides actionable wealth-growth advice based on user-defined (or standard 50/30/20) financial profiles.

## 👥 Agents Orchestrated
- `@[project-planner]`: Defining this 4-step execution strategy.
- `@[python-specialist]`: FastAPI routing, `pypdf` logic, ML Scikit-Learn pipelines.
- `@[database-architect]`: Schema updates for the User Financial DNA profiles.
- `@[backend-specialist]`: Java Spring Boot integration.
- `@[frontend-specialist]`: UI integration for the new Settings & ML advice.

---

## 🏗️ Phase 2: Implementation Breakdown

### 1. Token Optimization Pipeline (`main.py`)
- **PDF/CSV Routing:** Refactor `/api/ia/extrato` to explicitly branch logic. If the MIME type is `application/pdf`, bypass the Gemini generation completely and run it through `parsear_pdf_tabular()`.
- **Batch Categorization:** Gather all localized descriptions not found in `DICIONARIO_BASE`. Send a single `{"descricao": "MAP"}` request to Gemini. This slashes thousands of tokens per document.
- **Image Routing:** Retain the Gemini Vision (`gemini-2.5-flash`) engine for `.png` and `.jpg` inputs as agreed.

### 2. Behavioral Database Schema (Java)
- **Entity Update:** Add a new `@Entity` called `PerfilFinanceiro` (or expand `User`) to store:
  - `tipoPerfil` (`PADRAO`, `CUSTOMIZADO`)
  - `metaPoupancaMensal` (Percentage, default 20%)
  - `tetoGastosEssenciais` (Percentage, default 50%)
  - `tetoLivre` (Percentage, default 30%)
- **Data Layer:** Expand `UsuarioRepository` and the API layer to allow users to toggle between standard and custom modes.

### 3. ML Growth Engine & Leak Detection (Python)
- **Leak Detection:** Enhance the `IsolationForest` to specifically tag "bad manners" (e.g., highly recurring non-essential tiny transactions).
- **Growth Advisor Strategy:** Implement a new metric check mapping `Gastos_Atuais` vs `Meta_Perfil`. If the user is bleeding money in the "Lazer/Delivery" category preventing them from hitting their `metaPoupancaMensal`, the ML throws a structured alert.
- **LLM Context Injection:** Feed the ML anomaly results and the User's Profile parameters into Gemini to act as a harsh but fair Wealth Advisor.

### 4. UI/UX Pro-Max Integration (Frontend)
- **Financial DNA Menu:** Create a Liquid Glass config card on the Dashboard or Gestor for the User to set their standard/custom behavior mode.
- **Advisor Dashboard:** Render the ML "Bad Manners" warnings and "Growth Advice" in the AI insight board.

---

## 🛡️ Security & Validations
- **Tenant Validation:** Changes made to the financial profile must be strictly scoped to the JWT bearer.
- **Fail-Closed Strategy:** If the Gemini API fails during batch categorization, the local system must categorize unknown items as `OUTROS` instead of crashing.
