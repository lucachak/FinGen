package lucas.basemodel.modules.financeiro.services.strategies;

import lucas.basemodel.modules.financeiro.dto.DistribuicaoResult;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da Regra Foco em Poupança (40/40/20):
 * 40% - Necessidades
 * 20% - Lazer
 * 40% - Poupança/Metas
 */
@Component
public class FocoPoupancaStrategy implements DistribuicaoStrategy {

    @Override
    public EstrategiaDistribuicao getEstrategia() {
        return EstrategiaDistribuicao.FOCO_POUPANCA_40_40_20;
    }

    @Override
    public DistribuicaoResult calcular(BigDecimal renda) {
        BigDecimal necessidades = renda.multiply(new BigDecimal("0.40"));
        BigDecimal desejos = renda.multiply(new BigDecimal("0.20"));
        BigDecimal investimentos = renda.multiply(new BigDecimal("0.40"));

        List<DistribuicaoResult.ItemDistribuicao> orcamentos = new ArrayList<>();
        orcamentos.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Necessidades (40%)")
                .valor(necessidades)
                .build());
        orcamentos.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Desejos (20%)")
                .valor(desejos)
                .build());

        List<DistribuicaoResult.ItemDistribuicao> metas = new ArrayList<>();
        metas.add(DistribuicaoResult.ItemDistribuicao.builder()
                .nome("Investimentos (40%)")
                .valor(investimentos)
                .build());

        return DistribuicaoResult.builder()
                .orcamentos(orcamentos)
                .metas(metas)
                .build();
    }
}
