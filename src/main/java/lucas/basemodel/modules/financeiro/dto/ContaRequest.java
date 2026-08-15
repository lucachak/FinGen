package lucas.basemodel.modules.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.Prioridade;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ContaRequest {
    @NotBlank(message = "A descrição é obrigatória")
    private String descricao;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private BigDecimal valor;

    @NotNull(message = "A data de vencimento é obrigatória")
    private LocalDate dataVencimento;

    @NotNull(message = "O tipo é obrigatório")
    private TipoTransacao tipo;

    private EscopoTransacao escopo = EscopoTransacao.CASA;
    private Frequencia frequencia = Frequencia.AVULSA;
    private Prioridade prioridade = Prioridade.MEDIA;
    private Long categoriaId;
    private UUID assetId;
}
