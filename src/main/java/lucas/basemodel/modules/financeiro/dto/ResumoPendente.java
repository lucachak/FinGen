package lucas.basemodel.modules.financeiro.dto;

import java.math.BigDecimal;

/** Quantidade e soma de contas pendentes em um intervalo de vencimento. */
public record ResumoPendente(Long quantidade, BigDecimal total) {
}
