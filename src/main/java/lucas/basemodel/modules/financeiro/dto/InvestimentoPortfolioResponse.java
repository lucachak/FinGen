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
public class InvestimentoPortfolioResponse {
    private List<InvestimentoResponse> ativos;
    private BigDecimal totalAportado;
    private BigDecimal totalAtual;
    private BigDecimal roiTotal;
    private double roiPercentual;
}
