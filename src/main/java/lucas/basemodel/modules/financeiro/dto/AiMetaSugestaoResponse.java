package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMetaSugestaoResponse {
    private String titulo;
    private NaturezaMeta natureza;
    private BigDecimal valorAlvo;
    private BigDecimal aporteMensal;
    private int prazoMeses;
    private String justificativa;
    private String icone;
}
