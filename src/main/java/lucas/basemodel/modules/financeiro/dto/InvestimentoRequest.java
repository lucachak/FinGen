package lucas.basemodel.modules.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.TipoAtivo;

import java.math.BigDecimal;

@Data
public class InvestimentoRequest {
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotNull(message = "O tipo é obrigatório")
    private TipoAtivo tipo;

    @NotNull(message = "O valor aportado é obrigatório")
    @Positive(message = "O valor aportado deve ser positivo")
    private BigDecimal valorAportado;

    private BigDecimal valorAtual;
    private String ticker;
}
