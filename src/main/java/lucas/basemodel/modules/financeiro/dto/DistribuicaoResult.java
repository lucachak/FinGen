package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribuicaoResult {

    private List<ItemDistribuicao> orcamentos;
    private List<ItemDistribuicao> metas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDistribuicao {
        private String nome;
        private BigDecimal valor;
    }
}
