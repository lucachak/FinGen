package lucas.basemodel.modules.financeiro.services.strategies;

import lucas.basemodel.modules.financeiro.dto.DistribuicaoResult;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;

import java.math.BigDecimal;

/**
 * Interface para estratégias de distribuição de renda.
 */
public interface DistribuicaoStrategy {
    
    EstrategiaDistribuicao getEstrategia();
    
    DistribuicaoResult calcular(BigDecimal renda);
}
