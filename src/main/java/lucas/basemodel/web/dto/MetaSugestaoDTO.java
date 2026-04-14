package lucas.basemodel.web.dto;

import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import java.math.BigDecimal;

public record MetaSugestaoDTO(
    String titulo,
    BigDecimal valorAlvo,
    BigDecimal aporteMensal,
    int prazoMeses,
    NaturezaMeta natureza,
    String justificativa
) {}
