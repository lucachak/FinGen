package lucas.basemodel.modules.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrcamentoRequest {
    @NotNull(message = "Categoria é obrigatória")
    private Long categoriaId;

    @NotNull(message = "Limite mensal é obrigatório")
    @Positive(message = "O limite deve ser positivo")
    private BigDecimal limiteMensal;
}
