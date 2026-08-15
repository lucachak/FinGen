package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.TipoAtivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestimentoResponse {
    private UUID id;
    private String nome;
    private TipoAtivo tipo;
    private BigDecimal valorAportado;
    private BigDecimal valorAtual;
    private BigDecimal rentabilidade;
    private BigDecimal quantidade;
    private BigDecimal precoAtual;
    private String ticker;
    private LocalDate dataAtualizacao;
}
