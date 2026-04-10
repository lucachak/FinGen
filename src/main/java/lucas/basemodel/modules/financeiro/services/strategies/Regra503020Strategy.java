package lucas.basemodel.modules.financeiro.services.strategies;

import lucas.basemodel.modules.financeiro.dto.DistribuicaoResult;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da Regra 50/30/20:
 * 50% - Necessidades (Despesas Essenciais)
 * 30% - Desejos (Lazer e Estilo de Vida)
 * 20% - Dívidas ou Poupança (Investimentos)
 */
@Component
public class Regra503020Strategy implements DistribuicaoStrategy {

    @Override
    public EstrategiaDistribuicao getEstrategia() {
        return EstrategiaDistribuicao.REGRA_50_30_20;
    }

    @Override
    public DistribuicaoResult calcular(BigDecimal renda) {
        BigDecimal necessidades = renda.multiply(new BigDecimal("0.50"));
        BigDecimal desejos = renda.multiply(new BigDecimal("0.30"));
        BigDecimal investimentos = renda.multiply(new BigDecimal("0.20"));

        List<DistribuicaoResult.ItemDistribuicao> orcamentos = new ArrayList<>();
        orcamentos.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Despesas Essenciais")
                .valor(necessidades)
                .build());
        orcamentos.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Lazer e Estilo de Vida")
                .valor(desejos)
                .build());

        List<DistribuicaoResult.ItemDistribuicao> metas = new ArrayList<>();
        metas.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Reserva e Investimentos")
                .valor(investimentos)
                .build());

        return DistribuicaoResult.builder()
                .orcamentos(orcamentos)
                .metas(metas)
                .build();
    }
}
