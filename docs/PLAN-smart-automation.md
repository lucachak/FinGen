# PLAN-smart-automation

Objetivo: Reestruturar o módulo `financeiro` para suportar Automação Inteligente de Caixa usando o Padrão Template-Instance, distinguindo despesas fixas (Cron) de variáveis (previsão via Gemini IA).

## User Review Required (Socratic Gate)

> [!IMPORTANT]
> **Questões Arquiteturais Críticas antes de iniciar a Fase 1:**
> 
> 1. **O Paradoxo da Entidade `Conta`:** Atualmente no sistema, a classe `Conta.java` representa o **Lançamento/Despesa** (ex: Conta de Luz), e não a *Conta Bancária*. No seu prompt, você sugere criar `Transacao` contendo `conta (Conta)`. 
>    * Opção A: Renomeamos o atual `Conta.java` em todo o sistema para `Transacao` (o que é o ideal, abrindo espaço para uma futura entidade `ContaBancaria`).
>    * Opção B: Mantemos `Conta.java` como a Instance, e criamos `TransacaoRecorrente` (Template) referenciando-a internamente? Ou mantemos a nomenclatura confusa Conta contendo Conta?
>    *Qual caminho prefere? Sugiro a Opção A para alinhamento semântico longo prazo.*
> 
> 2. **Frequência vs Vencimento:** A IA deve analisar faturas flutuantes (Luz, Água). Se a data de vencimento de uma fatura variar (ex: mês passado dia 10, este mês dia 12), o agendador fixo (`diaVencimento` na Recorrente) deve inferir uma margem de D-5 dias para acionar a API do Gemini e gerar a transação como `PREVISTO_IA`? O que acha dessa abordagem D-5?

## Proposed Changes

### Phase 1: Modelagem de Dados
- **[NEW]** `GrupoRecorrencia.java`: Enum (FIXA, VARIAVEL).
- **[NEW]** `StatusTransacao.java`: Enum (PENDENTE, PAGO, PREVISTO_IA, CANCELADO).
- **[NEW]** `TransacaoRecorrente.java`: Entidade Template contendo titulo, tipo, grupo, frequencia, diaVencimento, valorBase, e automacaoAtiva.
- **[MODIFY]** `Transacao.java` (atual `Conta.java`): Adição de campos `valorPrevisto`, `valorRealizado`, `transacaoRecorrente_id`, `status` e UUID id (migração de Long para UUID ou manutenção de Long para estabilidade).

### Phase 2: Repositórios
- **[NEW]** `TransacaoRecorrenteRepository.java`: Consultas agendadas (`findByAutomacaoAtivaTrueAndDiaVencimento`).
- **[MODIFY]** `TransacaoRepository.java` (atual `ContaRepository`): Consultas para alimentar histórico de treino da IA (`findTopXByTransacaoRecorrenteAndStatusOrderByDataVencimentoDesc`).

### Phase 3: Serviço de Automação
- **[NEW]** `AutomacaoFinanceiraService.java`: 
  - `@Scheduled` cron job diário.
  - `processarContasFixas()`: Copia valorBase direto para a Transacao.
  - `preverContasVariaveis()`: Extrai últimos N pagamentos, injeta na interface da IA (preverVariacao) com as orientações do FinGen, e gera a Transacao com valor estimado.

## Verification Plan

### Automated Tests
- Teste unitário em `AutomacaoFinanceiraService` verificando se o fallback de média aritmética simples (ex: (100+120+110)/3) é acionado caso o Gemini lance exceção.

### Manual Verification
- Acessar o Dashboard/Controller, criar uma `TransacaoRecorrente` VARIAVEL simulando Energia Elétrica. Inserir 3 pagamentos diretos passados e invocar a rotina de prever próxima mensalidade. Confirmar que a margem foi sensível à variação (não uma mera média cega) usando a capacidade do LLM.
