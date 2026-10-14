# Relatório — dados e performance do FinGen

Data: 2026-10-14 · branch `perf/dados-e-performance` · JDK 17.0.20.1 · H2 em memória

## Resultado (GET /app/dashboard, 20.000 contas, 1 usuário)

| | Antes | Depois |
|---|---|---|
| Mediana de tempo | **607 ms** (10 medições: 430–916 ms) | **89, 115, 109 ms** (3 execuções; mais uma de 99 ms durante o desenvolvimento) |
| Statements SQL por requisição | 11 | 18 |

Leitura: o ganho é de aproximadamente **5 a 6 vezes** no tempo. O número de statements **subiu**
(11 → 18), porque o que era uma query pesada que trazia todas as contas virou várias queries pequenas
com agregação e `LIMIT`. O custo antes estava em carregar e hidratar 20 mil entidades (com a descrição
descriptografada) para somar em memória.

Limites da medição: é uma requisição em processo (MockMvc) sobre H2, com o `EntityManager` limpo a cada
iteração; inclui renderização do template. Não inclui rede nem Postgres. Os valores absolutos em produção
serão diferentes; a proporção é o que importa. A listagem paginada da API (`findForApi`) já era paginada
e **não foi medida**.

## O que mudou

| Área | Mudança |
|---|---|
| Consultas | `DashboardQueryService` (agregação `SUM … GROUP BY`, listas com `LIMIT`, intervalos de data). `DashboardController`, `DashboardService` e `FinancialHealthService` passaram a usá-lo. |
| Queries | `MONTH()/YEAR()` substituídos por `>= inicio AND < fimExclusivo` em 3 queries. |
| Índices | 3 índices compostos em `contas`, declarados com `@Index` (criados pelo `ddl-auto=update`). |
| N+1 | `OrcamentoService.listarComConsumo`: 1 consulta agregada em vez de 1 por orçamento. |
| `findAll()` | `AiService.chat` usa consulta por responsável; `ContaService.listarTodas` removido (sem chamadores). |
| Relações | `Orcamento.categoria` passou a `LAZY` (+ `EntityGraph` nas consultas), `default_batch_fetch_size=50`. |
| HTTP | Compressão de respostas habilitada. |
| Repositório | 5 logs e `teste.pdf` fora do versionamento (continuam em disco). |

## Verificação

- Teste de equivalência (`DashboardQueryServiceEquivalenciaTest`, 7 casos): os números do SQL são
  idênticos aos da lógica em memória anterior, por escopo, incluindo "Outros", usuário sem contas e
  isolamento entre usuários. Fica como guarda de regressão.
- Testes de borda do repositório (`ContaRepositoryAgregacaoTest`, 9 casos): virada de mês e de ano,
  paga sem data de pagamento, limite e ordem das listas.
- Suíte completa: **88 testes, 0 falhas** (baseline era 58).

## Decisões e desvios

- **Flyway não foi habilitado.** Roda antes do Hibernate; num banco novo, `V2`/`V3` fariam `ALTER` em
  tabelas inexistentes. Os índices vêm de `@Index` + `ddl-auto=update`. (Conclusão do desenho; não foi
  testado contra um banco vazio.)
- **Cada consumidor manteve seu comportamento** (API e HTML), em vez de "o controller vence".
- **`open-in-view` mantido no padrão (true).** A suíte passa 88/88 com ele desligado, mas os testes
  renderizam só 5 das 28 páginas; isso não prova que as outras aguentam.
- **`FinanceAiService` continua usando `findAll()` de usuários**: `nomeCompleto` usa
  `RandomEncryptionConverter` (IV aleatório), então uma consulta de igualdade no banco nunca casaria.
- **JDK:** os comandos rodaram com JDK 17 (`JAVA_HOME`). No JDK 27 padrão do sistema, o Lombok quebra no
  `testCompile` (`EndPosTable`). O `pom.xml` mira Java 17 (o README diz 21, o que está incorreto).

## Pendências

- **Primeiro deploy:** o `ddl-auto=update` criará os 3 índices no Postgres da Render na subida. Em
  tabela grande isso é um `CREATE INDEX` não concorrente (bloqueia escritas enquanto roda). Vale conferir
  o tamanho de `contas` antes, ou criar os índices manualmente com `CONCURRENTLY`.
- `ContaController` (lista de contas) ainda carrega a lista completa e filtra em memória.
- `ContaService` ainda usa listas do mês corrente para donut/gastos por morador (limitado a um mês).
- `HistoricoPatrimonioService` e `AdminController` usam `findAll()` de usuários.
- Busca por nome de usuário no `FinanceAiService` exigiria uma coluna com hash determinístico.
- `open-in-view=false`: depende de testes de renderização das demais telas.
