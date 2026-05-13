package lucas.basemodel.modules.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MetaRequest {
    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    private NaturezaMeta natureza = NaturezaMeta.OUTROS;

    @NotNull(message = "Valor alvo é obrigatório")
    @Positive(message = "O valor alvo deve ser positivo")
    private BigDecimal valorAlvo;

    private BigDecimal valorAtual = BigDecimal.ZERO;
    private LocalDate prazo;
    private String icone;
}
