# PLAN-route-refactoring

## 🎯 Objetivo
Implementar uma taxonomia estrita e RESTful (mesmo para MVC) para resolver inconsistências e organizar o FinGen.

## 🗺️ Mapeamento de Rotas (DE -> PARA)

### 1. Rotas Públicas (`/` e `/auth/...`)
- **HomeController**: `GET /` -> Mantém `@GetMapping("/")` (Landing Page/Marketing)
- **AuthController**: Várias rotas soltas (`/login`, `/cadastro`) -> Redirecionar para escopo de Autenticação `@RequestMapping("/auth")`.

### 2. Dashboards e Funcionalidades Core (`/app/...`)
- **GestorController**: `@RequestMapping("/gestor")` -> `@RequestMapping("/app/dashboard")`
- **DistribuicaoSetupController**: `@RequestMapping("/financeiro/setup")` -> `@RequestMapping("/app/financeiro/setup")`
- **ContaController**: `@RequestMapping("/contas")` -> `@RequestMapping("/app/financeiro/contas")`
- **TransacaoController**: `@RequestMapping("/transacoes")` -> `@RequestMapping("/app/financeiro/transacoes")`
- **CategoriaController**: `@RequestMapping("/categorias")` -> `@RequestMapping("/app/financeiro/categorias")`
- **OrcamentoController**: `@RequestMapping("/orcamentos")` -> `@RequestMapping("/app/financeiro/orcamentos")`
- **MetaController**: `@RequestMapping("/metas")` -> `@RequestMapping("/app/financeiro/metas")`
- **TransacaoRecorrenteController**: `@RequestMapping("/recorrentes")` -> `@RequestMapping("/app/financeiro/recorrentes")`
- **RelatorioController**: `@RequestMapping("/relatorios")` -> `@RequestMapping("/app/financeiro/relatorios")`

### 3. Patrimônio / Wealth (`/app/wealth/...`)
- **WealthController**: Variado -> `@RequestMapping("/app/wealth/dashboard")` (ou `/app/wealth/ativos`)
- **InvestimentoController**: `@RequestMapping("/investimentos")` -> `@RequestMapping("/app/wealth/investimentos")`
- **WealthSetupController**: `@RequestMapping("/wealth/setup")` -> `@RequestMapping("/app/wealth/setup")`

### 4. Configurações e Perfil (`/app/settings/...`)
- **MoradorController**: `@RequestMapping("/moradores")` -> `@RequestMapping("/app/settings/moradores")`
- **UserController**: Edição de perfil e preferências -> Base `@RequestMapping("/app/settings/perfil")`

### 5. API e Assíncrono (`/api/v1/...`)
- **AiController**: `@RequestMapping("/api/ia")` -> `@RequestMapping("/api/v1/ia")`
- **BaseApiController**: `@RequestMapping("/api/v1")` -> Mantém-se (Padrão ouro).

## 🛠️ Próximas Fases (Após Aprovação)

### Phase 2: Refatoração do Backend
- Aplicação das tags `@RequestMapping` listadas acima nas classes `*Controller.java`.
- Correção cuidadosa de todas as chamadas de redirecionamento `redirect:` para os novos endpoints (inclusive entre fluxos como `financeiro/setup` chamando `financeiro/contas/nova`).
- Nenhuma View e seu caminho lógico de disco (pastas e arquivos `.html`) será alterada.

### Phase 3: Refatoração do Frontend
- Varredura em `/src/main/resources/templates/` atrás de referências a URLs.
- Correção de links (`th:href="@{...}"`), submissões (`th:action="@{...}"`) e requisições Fetch (`fetch('/api/v1/...')`).
- Revisão sensível de fragmentos centrais: `<nav>` nos arquivos `navbar.html` / `sidebar.html`.

### Phase 4: Refatoração de Segurança
- Ajustes críticos em `SecurityConfig.java`:
  - Liberação de estáticos, `/` e rotas `/auth/**`.
  - Proteção compulsória via configuração global para os contextos `/app/**` e `/api/v1/**`.
