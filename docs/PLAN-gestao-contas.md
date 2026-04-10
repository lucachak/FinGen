# Plan: Gestao de Contas UI & Recurrent API

## 1. Context Check (Phase -1)
- **Goal:** Upgrade Gestão de Contas UI/UX and introduce an editable isolation flow for AI-extracted transactions, plus recurrence detection.
- **Scope:** `contas/lista.html`, `main.py`, `AiController.java`, `GeminiService.java`
- **Tech Stack:** Spring Boot, HTMX, Tailwind/CSS variables, Python FastAPI, Gemini 2.5 Pro Multimodal.

## 2. Problem Statement
1. The **Gestão de Contas** page lacks a "Pro-Max" presentation for dense data and editing.
2. The AI Extractor saves/processes transactions blindly. The user wants a staging area where each transaction "is isolated as a single entity" and can be edited *before* finalizing.
3. The AI (`main.py`) does not return whether a bill/transaction is recurrent (weekly/monthly).

## 3. Proposed Socratic Questions for User
Before I write any code, we must lock in the design and flow:
1. **Editable Staging Area:** When you upload the PDF/Photo, should the AI parsed results appear inside a modal overlay or replace the current screen list for editing?
2. **Recurrence Logic:** If `main.py` flags a transaction as "MENSAL" (Monthly), should the Java backend automatically create 12 future `Conta` records, or do you just want it tagged visually for now? 
3. **UI/UX Direction (Pro-Max):** By "see all the info properly", do you prefer a dense Data Table (like Excel) or expanded thick Cards (like Apple Wallet) for each line?

## 4. Proposed Technical Implementation (Phase 2 & 3)
### A. Backend (`main.py` & Java)
- **main.py:** Update `ExtratoResponseSchema` in `main.py` to add `frequencia: Literal['AVULSA', 'MENSAL', 'SEMANAL', 'ANUAL']`. Update the prompt to intelligently guess this based on the bill desc (e.g., Netflix = MENSAL).
- **Java:** Update `GeminiService` to map this new field. Update `AiController` to return an HTML fragment (`<form th:each="...">`) instead of saving directly to DB, allowing the user to view and edit inputs before sending a massive `POST /contas/salvar-em-lote`.

### B. Frontend (`contas/lista.html` & `gestor/index.html`)
- Implement the "UI-UX Pro-Max" standards for `contas/lista.html`:
  - Hover states without layout shifts.
  - Premium Typography and spacing.
  - Interactive "Edit" buttons that open inline editing forms.
- Create `contas/staging.html` fragment to render the editable AI output.

## 5. Verification
- `test_runner.py` for API logic.
- `ux_audit.py` for the new UI.
- `security_scan.py` for the API surface.



