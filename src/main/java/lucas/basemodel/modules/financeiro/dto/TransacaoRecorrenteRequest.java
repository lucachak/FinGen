package lucas.basemodel.modules.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.GrupoRecorrencia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;

import java.math.BigDecimal;

@Data
public class TransacaoRecorrenteRequest {
    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    @NotNull(message = "O tipo é obrigatório")
    private TipoTransacao tipo;

    @NotNull(message = "A frequência é obrigatória")
    private Frequencia frequencia;

    @NotNull(message = "O grupo é obrigatório")
    private GrupoRecorrencia grupo;

    @NotNull(message = "O dia de vencimento é obrigatório")
    private Integer diaVencimento;

    @Positive(message = "O valor base deve ser positivo")
    private BigDecimal valorBase;

    private Long categoriaId;

    @NotNull(message = "O espaço financeiro é obrigatório")
    private EscopoTransacao escopo = EscopoTransacao.PESSOAL;
}
