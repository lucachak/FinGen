package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoResponse {
    private UUID id;
    private Long categoriaId;
    private String categoriaNome;
    private BigDecimal limiteMensal;
    private BigDecimal gastoAtual;
    private double percentualConsumo;
    private boolean geradoPeloSistema;
}
