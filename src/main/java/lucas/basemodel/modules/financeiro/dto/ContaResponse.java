package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.Prioridade;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaResponse {
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private boolean paga;
    private TipoTransacao tipo;
    private StatusTransacao status;
    private EscopoTransacao escopo;
    private Frequencia frequencia;
    private Prioridade prioridade;
    private String categoriaNome;
    private String comprovante;
    private UUID assetId;
}
