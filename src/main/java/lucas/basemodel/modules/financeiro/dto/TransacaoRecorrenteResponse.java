package lucas.basemodel.modules.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.GrupoRecorrencia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoRecorrenteResponse {
    private UUID id;
    private String titulo;
    private TipoTransacao tipo;
    private Frequencia frequencia;
    private GrupoRecorrencia grupo;
    private Integer diaVencimento;
    private BigDecimal valorBase;
    private String categoriaNome;
    private boolean automacaoAtiva;
}
