package lucas.basemodel.modules.financeiro.enums;

/**
 * Define as estratégias de distribuição automática de renda para orçamentos e metas.
 */
public enum EstrategiaDistribuicao {
    REGRA_50_30_20,        // 50% Essenciais, 30% Lazer, 20% Poupança
    FOCO_POUPANCA_40_40_20, // 40% Essenciais, 20% Lazer, 40% Poupança
    MANUAL                  // Distribuição manual pelo usuário
}
