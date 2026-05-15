# 💸 FinGen — Gestão Financeira Inteligente

> Plataforma web completa para controle financeiro doméstico e pessoal, com inteligência artificial integrada, gestão de patrimônio e orçamentos automatizados.

[![Live Demo](https://img.shields.io/badge/Live_Demo-FinGen-4CAF50?style=for-the-badge&logo=render)](https://fingen-app.onrender.com)
[![GitHub Repo](https://img.shields.io/badge/GitHub-lucachak/FinGen-black?style=for-the-badge&logo=github)](https://github.com/lucachak/FinGen)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)

---

## 📖 Sobre o Projeto

O **FinGen** é uma aplicação web full-stack desenvolvida para simplificar e centralizar a gestão financeira familiar e pessoal. Combina um backend robusto em Spring Boot com um frontend reativo via HTMX e Thymeleaf, oferecendo uma experiência fluida sem a complexidade de um SPA separado.

A plataforma vai além do simples controle de despesas: integra **IA generativa** (Google Gemini + OpenRouter) para análise de extratos bancários, sugestão de metas financeiras e consultoria personalizada, além de um módulo completo de **gestão de patrimônio** com rastreamento de ativos (imóveis, ações, veículos, renda passiva).

---

## ✨ Funcionalidades Principais

### 💳 Gestão Financeira (Módulo Core)
- **Contas a Pagar/Receber** — Registro completo de despesas e receitas com categorias, prioridades e comprovantes em anexo
- **Escopos de Transação** — Separação por escopo: `CASA`, `PESSOAL` e `NEGÓCIO`
- **Pagamento Rápido** — Marcar conta como paga com um clique, sem abrir o formulário
- **Transações Recorrentes** — Automação de gastos mensais/semanais/anuais por grupo de recorrência
- **Histórico de Transações** — Listagem completa com filtros de status (atrasada, a vencer, paga)

### 📊 Dashboard Centralizado
- **Cards de KPIs mensais** — Gastos Casa, Pessoal e Negócio com valor pendente em destaque
- **Free Cash Flow** — Cálculo em tempo real: Receitas − (Despesas realizadas + Pendentes)
- **Gráfico Doughnut** — Distribuição de gastos da casa por categoria no mês atual
- **Histórico de Patrimônio** — Gráfico de linha com evolução do patrimônio líquido (até 12 meses)
- **Alertas de Orçamento** — Notificações automáticas quando 80%+ do limite de uma categoria é atingido
- **Metas em Andamento** — Progresso visual das metas financeiras ativas
- **Próximas Contas** — Lista das próximas despesas pendentes
- **Transações Recentes** — Últimas 5 transações pagas
- **Sugestões de IA** — Insights gerados automaticamente com base no snapshot patrimonial

### 🤖 Inteligência Artificial (Dupla Stack)
| Serviço | Função |
|---------|--------|
| **Google Gemini** | Processamento de extratos bancários (PDF/imagem), análise de anomalias de gastos, plano de investimentos personalizado |
| **OpenRouter** | Chat financeiro conversacional, sugestão de metas baseada no perfil do usuário |

**Fluxo de Importação de Extrato:**
1. Upload de PDF/imagem do extrato bancário
2. Gemini extrai e classifica automaticamente as transações
3. Usuário revisa, edita e confirma as transações em staging
4. Transações confirmadas são salvas no banco de dados

### 🏦 Gestão de Patrimônio (Wealth Module)
- **Tipos de Ativos Suportados:**
  - `BankAccountAsset` — Contas bancárias e caixas
  - `StockAsset` — Ações e fundos de investimento (com sync de cotações de mercado)
  - `RealEstateAsset` — Imóveis
  - `VehicleAsset` — Veículos
  - `IncomeAsset` — Rendas passivas mensais
- **Wealth Snapshot** — Foto instantânea do patrimônio total com breakdown por tipo de ativo
- **Histórico Patrimonial** — Evolução do patrimônio líquido ao longo do tempo
- **Sugestões Automáticas de IA** — Geradas a cada snapshot com base na composição de ativos

### 🎯 Metas Financeiras
- Criação manual de metas com natureza, valor-alvo, prazo e progresso
- **Naturezas de Meta:** `VIAGEM`, `CARRO`, `CASA`, `APOSENTADORIA`, `RESERVA_EMERGENCIA`, `EDUCACAO`, `OUTRO`
- Cálculo automático de aporte mensal necessário para atingir a meta no prazo
- **Sugestão de Metas via IA** — OpenRouter sugere metas baseado no perfil financeiro do usuário
- **Criação de Meta pela IA** — Um clique converte a sugestão em meta salva

### 📈 Investimentos
- Rastreamento de carteira de investimentos com valor aportado e valor atual
- **Tipos de Ativo:** Tesouro Direto, CDB, LCI/LCA, Ações, FIIs, Fundos, Cripto, Poupança, Outros
- Cálculo de ROI total da carteira
- **Sync de Cotações** — Atualização automática de preços de mercado via `MercadoService`

### 📋 Orçamentos por Categoria
- Definição de limite mensal de gasto por categoria
- Monitoramento em tempo real do consumo vs. limite (%)
- **Status de Risco:** `NORMAL` (< 80%), `ALERTA` (80–99%), `CRÍTICO` (≥ 100%)
- **Geração Automática** — Cria orçamentos baseados na média dos últimos 3 meses de gastos (+10% de margem)

### 🏠 Gestão de Moradores
- Suporte a múltiplos usuários no mesmo lar
- Rateio de despesas entre moradores
- Upload de foto de perfil
- Controle de ativação/desativação (soft delete para preservar histórico)

### 📝 Categorias
- CRUD completo de categorias de transação
- Naturezas de categoria para organização contábil

### 📊 Relatórios
- Módulo de relatórios financeiros (`/app/financeiro/relatorios`)

### 🚀 Onboarding
- Fluxo guiado de configuração inicial do perfil financeiro
- Coleta de: perfil financeiro, estratégia de orçamento, meta de poupança, teto de gastos essenciais
- Setup de distribuição financeira com suporte a múltiplas estratégias (`EstrategiaDistribuicao`)
- Redireciona para o dashboard apenas após a conclusão do setup

---

## 🗺️ Mapa de Rotas

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/` | Página inicial (redireciona para `/app/dashboard`) |
| `GET` | `/auth/login` | Página de login |
| `GET/POST` | `/auth/register` | Cadastro de novo usuário |
| **Dashboard** | | |
| `GET` | `/app/dashboard` | Dashboard principal |
| `GET` | `/app/dashboard/chart-data` | Dados JSON para gráficos (HTMX) |
| **Contas** | | |
| `GET` | `/app/financeiro/contas` | Lista de contas pendentes e histórico |
| `GET` | `/app/financeiro/contas/nova` | Formulário de nova conta |
| `GET` | `/app/financeiro/contas/editar/{id}` | Edição de conta |
| `POST` | `/app/financeiro/contas/salvar` | Salvar conta (com upload de comprovante) |
| `POST` | `/app/financeiro/contas/pagar/{id}` | Pagamento rápido |
| `POST` | `/app/financeiro/contas/excluir/{id}` | Excluir conta |
| `POST` | `/app/financeiro/contas/salvar-lote` | Importação em lote (via IA) |
| **Transações Recorrentes** | | |
| `GET` | `/app/financeiro/recorrentes` | Lista de automações |
| `GET` | `/app/financeiro/recorrentes/nova` | Nova automação |
| `GET` | `/app/financeiro/recorrentes/editar/{id}` | Editar automação |
| `POST` | `/app/financeiro/recorrentes/salvar` | Salvar automação |
| `POST` | `/app/financeiro/recorrentes/excluir/{id}` | Excluir automação |
| **Metas** | | |
| `GET` | `/app/financeiro/metas` | Lista de metas |
| `GET` | `/app/financeiro/metas/novo` | Nova meta |
| `GET` | `/app/financeiro/metas/editar/{id}` | Editar meta |
| `POST` | `/app/financeiro/metas/salvar` | Salvar meta |
| `POST` | `/app/financeiro/metas/excluir/{id}` | Excluir meta |
| `POST` | `/app/financeiro/metas/ai-suggest` | Sugestões de metas via IA (JSON) |
| `POST` | `/app/financeiro/metas/ai-criar` | Criar meta a partir de sugestão da IA |
| **Orçamentos** | | |
| `GET` | `/app/financeiro/orcamentos` | Lista de orçamentos por categoria |
| `GET` | `/app/financeiro/orcamentos/novo` | Novo orçamento |
| `GET` | `/app/financeiro/orcamentos/editar/{id}` | Editar orçamento |
| `POST` | `/app/financeiro/orcamentos/salvar` | Salvar orçamento |
| `POST` | `/app/financeiro/orcamentos/excluir/{id}` | Excluir orçamento |
| `POST` | `/app/financeiro/orcamentos/gerar-automatico` | Gerar orçamentos automaticamente |
| **Relatórios** | | |
| `GET` | `/app/financeiro/relatorios` | Relatórios financeiros |
| **Investimentos** | | |
| `GET` | `/app/wealth/investimentos` | Carteira de investimentos |
| `GET` | `/app/wealth/investimentos/novo` | Novo investimento |
| `GET` | `/app/wealth/investimentos/editar/{id}` | Editar investimento |
| `POST` | `/app/wealth/investimentos/salvar` | Salvar investimento |
| `POST` | `/app/wealth/investimentos/excluir/{id}` | Excluir investimento |
| `POST` | `/app/wealth/investimentos/sync` | Sincronizar cotações de mercado |
| **IA (Assistente)** | | |
| `GET` | `/app/ia` | Interface do assistente IA |
| `GET` | `/app/ia/revisar` | Revisão de extrato importado |
| `POST` | `/api/ia/chat` | Chat com IA (OpenRouter) |
| `POST` | `/api/ia/upload-extrato` | Upload de extrato para processamento |
| `POST` | `/api/ia/confirmar` | Confirmar importação de transações |
| `GET` | `/api/ia/consultor-pessoal` | Plano de investimentos personalizado |
| `GET` | `/api/ia/analisar-anomalias` | Análise de anomalias (HTMX fragment) |
| `GET` | `/api/ia/status` | Status do serviço de IA |
| **Configurações** | | |
| `GET` | `/app/settings/moradores` | Lista de moradores |
| `GET` | `/app/settings/moradores/novo` | Adicionar morador |
| `GET` | `/app/settings/moradores/editar/{id}` | Editar morador |
| `POST` | `/app/settings/moradores/salvar` | Salvar morador |
| `POST` | `/app/settings/moradores/remover/{id}` | Desativar morador |
| **Categorias** | | |
| `GET` | `/app/categorias` | Lista de categorias |
| **Wealth API (REST)** | | |
| `GET` | `/api/v1/wealth/summary` | Resumo patrimonial (JSON) |
| `GET` | `/api/v1/wealth/history` | Histórico de snapshots (JSON) |
| `POST` | `/api/v1/wealth/assets` | Adicionar ativo (JSON) |
| **Onboarding** | | |
| `GET/POST` | `/app/onboarding` | Configuração inicial do perfil |
| `GET/POST` | `/app/setup/distribuicao` | Setup de estratégia de distribuição |

---

## 🏗️ Arquitetura

```
FinGen
├── Java Backend (Spring Boot)          → Porta 8080
│   ├── Web Layer (Controllers + Thymeleaf/HTMX)
│   ├── Service Layer (Business Logic)
│   ├── Repository Layer (Spring Data JPA)
│   └── Security Layer (Spring Security + JWT)
│
├── Python IA Service (FastAPI)         → Porta 8000
│   └── Gemini API integration (extrato processing)
│
└── Database
    ├── Supabase (Produção/Cloud - PostgreSQL)
    └── PostgreSQL (Desenvolvimento local via Docker)
```

### Estrutura de Pacotes Java

```
lucas.basemodel/
├── BaseModelApplication.java          # Entry point
├── core/
│   ├── config/                        # Spring configuration beans
│   ├── exceptions/                    # Global exception handlers
│   ├── mail/                          # E-mail service
│   ├── security/                      # Spring Security config + JWT
│   └── storage/                       # File storage service
├── modules/
│   ├── auth/                          # Auth module (login, registro)
│   ├── financeiro/
│   │   ├── controllers/               # Onboarding & setup controllers
│   │   ├── dto/                       # Data Transfer Objects
│   │   ├── enums/                     # Domínio enumerável
│   │   ├── models/                    # Entidades JPA
│   │   ├── repositories/              # Spring Data repos
│   │   └── services/                  # Business logic
│   ├── user/                          # User entity + repository
│   └── wealth/
│       ├── controllers/               # Wealth API REST
│       ├── dto/
│       ├── enums/
│       ├── models/                    # Asset types + WealthSnapshot
│       ├── repositories/
│       └── services/
└── web/                               # MVC Controllers (UI)
    ├── dto/                           # View-specific DTOs
    ├── AIViewController.java
    ├── AiController.java
    ├── CategoriaController.java
    ├── ContaController.java
    ├── DashboardController.java
    ├── HomeController.java
    ├── InvestimentoController.java
    ├── MetaController.java
    ├── MoradorController.java
    ├── OrcamentoController.java
    ├── RelatorioController.java
    ├── TransacaoController.java
    └── TransacaoRecorrenteController.java
```

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| **Runtime** | Java (OpenJDK) | 21 |
| **Framework** | Spring Boot | 3.3.0 |
| **Persistência** | Spring Data JPA + Hibernate | — |
| **Banco (Dev)** | PostgreSQL | 15 |
| **Banco (Prod)** | Supabase (PostgreSQL) | — |
| **Templates** | Thymeleaf + Extras Spring Security 6 | — |
| **Reatividade** | HTMX (htmx-spring-boot-thymeleaf) | 3.6.0 |
| **Segurança** | Spring Security + JWT (JJWT) | 0.11.5 |
| **Utilitários** | Lombok | 1.18.36 |
| **Build** | Maven Wrapper (mvnw) | 3.9.6 |
| **Contêiner** | Docker (multi-stage build) | — |
| **IA (Java)** | Google Gemini API (via GeminiService) | — |
| **IA (Chat)** | OpenRouter API (via OpenRouterService) | — |
| **IA (Python)** | FastAPI + Google Gemini | — |
| **Deploy** | Render.com | — |

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/) instalados
- *Alternativa sem Docker:* Java 21, Maven 3.9+

### 1. Clone o repositório
```bash
git clone https://github.com/lucachak/FinGen.git
cd FinGen
```

### 2. Configure as variáveis de ambiente
Crie um arquivo `.env` na raiz do projeto:

```env
# Banco de Dados (Supabase - IPv4 Pooler)
SUPABASE_DB_URL=jdbc:postgresql://[pooler-host]:6543/postgres?prepareThreshold=0&ssl=true&sslmode=require
SUPABASE_DB_USER=postgres.[project-id]
SUPABASE_DB_PASS=sua_senha_do_banco

# Integração IA
GEMINI_TOKEN=seu_token_gemini
OPENROUTER_API_KEY=sua_chave_openrouter
PYTHON_API_URL=http://python-ia:8000

# Upload de Arquivos
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=10MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=10MB
```

> ⚠️ **Nunca commit o arquivo `.env`** — ele já está no `.gitignore`.

### 3. Suba os contêineres
```bash
docker-compose up --build
```

A aplicação estará disponível em: **http://localhost:8080**

### 4. (Opcional) Executar sem Docker

```bash
# Apenas o banco e o serviço Python via Docker
docker-compose up db python-ia -d

# Compile e execute o Java localmente
./mvnw spring-boot:run
```

---

## 🌐 Deploy em Produção (Render)

O projeto usa o arquivo `render.yaml` para deploy automático no [Render.com](https://render.com/).

### Serviços configurados:
| Serviço | Tipo | Dockerfile |
|---------|------|------------|
| `fingen-java` | Web Service | `./Dockerfile` |
| `fingen-ia` | Web Service | `./Dockerfile.python` |

### Variáveis de ambiente necessárias no painel Render:
| Variável | Descrição |
|----------|-----------|
| `OPENROUTER_API_KEY` | Chave da API OpenRouter para o chat IA |
| `GEMINI_TOKEN` | Token da API Google Gemini |
| `DB_URL` | URL do banco Supabase (JDBC) |
| `DB_USERNAME` | Usuário do pooler Supabase |
| `DB_PASSWORD` | Senha do banco Supabase |
| `PYTHON_API_URL` | URL interna do serviço Python IA |

> 💡 O banco de dados em produção foi migrado de H2 para **Supabase (PostgreSQL)**, garantindo persistência real e escalabilidade na nuvem.

---

## 🗄️ Modelo de Dados Principal

### Entidades Core

**`Conta`** — Unidade central de transação financeira
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `Long` | PK autoincremental |
| `descricao` | `String` | Descrição da transação |
| `valor` | `BigDecimal` | Valor da transação |
| `tipo` | `TipoTransacao` | `RECEITA` ou `DESPESA` |
| `escopo` | `EscopoTransacao` | `CASA`, `PESSOAL`, `NEGOCIO` |
| `prioridade` | `Prioridade` | `ALTA`, `MEDIA`, `BAIXA` |
| `frequencia` | `Frequencia` | `AVULSA`, `MENSAL`, `SEMANAL`, `ANUAL`, etc. |
| `status` | `StatusTransacao` | `PENDENTE`, `PAGO`, `ATRASADO` |
| `paga` | `boolean` | Flag de pagamento |
| `dataVencimento` | `LocalDate` | Data de vencimento |
| `dataPagamento` | `LocalDate` | Data efetiva de pagamento |
| `categoria` | `Categoria` (FK) | Categoria da transação |
| `responsavel` | `User` (FK) | Usuário responsável |
| `asset` | `Asset` (FK) | Ativo associado (opcional) |
| `comprovante` | `String` | Caminho do arquivo comprovante |

**`User`** — Perfil do usuário/morador
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `UUID` | PK gerada |
| `email` | `String` | E-mail único (login) |
| `username` | `String` | Nome de usuário único |
| `orcamentoMensal` | `BigDecimal` | Orçamento mensal (padrão: 3500.00) |
| `tipoPerfilFinanceiro` | `String` | Perfil de risco (`CONSERVADOR`, etc.) |
| `budgetingStrategy` | `WealthStrategy` | Estratégia de orçamento |
| `metaPoupancaMensal` | `BigDecimal` | % da renda para poupar (padrão: 20%) |
| `tetoGastosEssenciais` | `BigDecimal` | % teto para gastos essenciais (padrão: 50%) |
| `setupCompleted` | `boolean` | Flag de onboarding concluído |

---

## 🔐 Segurança

- **Autenticação:** Spring Security com sessão baseada em formulário + suporte a JWT
- **Autorização:** Rotas `/app/**` requerem autenticação
- **Isolamento de Dados:** Todos os controllers filtram dados pelo usuário autenticado via `Principal`
- **Senhas:** Armazenadas com `PasswordEncoder` (BCrypt)
- **Soft Delete:** Moradores são desativados (`ativo = false`) em vez de excluídos, preservando integridade histórica
- **Upload Seguro:** Arquivos recebem nome UUID antes de serem persistidos
- **Session Staging:** Cache em memória (`ConcurrentHashMap`) para staging de extratos, evitando bloat de sessão HTTP

---

## 🧪 Testes

```bash
# Executar todos os testes
./mvnw test

# Executar com relatório de cobertura
./mvnw verify
```

---

## 📁 Estrutura de Arquivos Importantes

```
FinGen/
├── src/
│   ├── main/
│   │   ├── java/lucas/basemodel/     # Código-fonte Java
│   │   └── resources/
│   │       ├── templates/            # Templates Thymeleaf
│   │       │   ├── auth/             # Login & registro
│   │       │   ├── dashboard/        # Dashboard principal
│   │       │   ├── contas/           # Gestão de contas
│   │       │   ├── metas/            # Metas financeiras
│   │       │   ├── orcamentos/       # Orçamentos por categoria
│   │       │   ├── investimentos/    # Carteira de investimentos
│   │       │   ├── recorrentes/      # Transações recorrentes
│   │       │   ├── moradores/        # Gestão de moradores
│   │       │   ├── ia/               # Interface do assistente IA
│   │       │   ├── wealth/           # Gestão de patrimônio
│   │       │   ├── relatorios/       # Relatórios
│   │       │   └── layout/           # Layout base (layout.html)
│   │       ├── static/               # CSS, JS, imagens
│   │       └── application.properties
│   └── test/                         # Testes automatizados
├── main.py                           # Serviço Python (Gemini IA)
├── requirements.txt                  # Dependências Python
├── Dockerfile                        # Build do serviço Java
├── Dockerfile.python                 # Build do serviço Python IA
├── docker-compose.yml                # Orquestração local (Java + Python + PostgreSQL)
├── render.yaml                       # Deploy em produção (Render.com)
├── pom.xml                           # Dependências Maven
└── .env                              # Variáveis de ambiente (não versionado)
```

---

## 🤝 Contribuindo

1. Faça um fork do repositório
2. Crie uma branch para sua feature: `git checkout -b feature/minha-feature`
3. Commit suas alterações: `git commit -m 'feat: adiciona nova funcionalidade'`
4. Push para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.

---

<div align="center">
  Feito com ☕ e Java por <a href="https://github.com/lucachak">Lucas Lucachak</a>
</div>