# Baseline — GET /app/dashboard (antes das otimizações)

Data: 2026-10-14 · JDK 17.0.20.1 (JAVA_HOME; o JDK padrão 27 quebra o Lombok no testCompile) · H2 em memória
Dataset: 20.000 contas, 36 meses, 3 escopos, 1 usuário (ContaDatasetFactory), EntityManager limpo a cada requisição.

    contas=20000
    jdk=17.0.20.1
    statements_por_requisicao=11
    mediana_ms=607
    tempos_ms=[430, 455, 460, 481, 576, 607, 613, 671, 725, 916]

Leitura: poucos statements (11) e ~600 ms: o custo está em carregar e hidratar todas as contas
(com descrição descriptografada) para agregar em memória, não em N+1.
