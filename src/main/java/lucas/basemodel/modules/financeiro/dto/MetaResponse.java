package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaResponse {
    private UUID id;
    private String titulo;
    private NaturezaMeta natureza;
    private BigDecimal valorAlvo;
    private BigDecimal valorAtual;
    private LocalDate prazo;
    private String status;
    private String icone;
    private double percentualConcluido;
    private boolean geradoPeloSistema;
}
