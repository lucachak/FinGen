package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private EscopoTransacao escopo;
    private BigDecimal gastosCasa;
    private BigDecimal gastosPessoal;
    private BigDecimal gastosNegocio;
    private BigDecimal freeCashFlow;
    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal patrimonioLiquido;
    private BigDecimal totalDespesasPendentes;
}
