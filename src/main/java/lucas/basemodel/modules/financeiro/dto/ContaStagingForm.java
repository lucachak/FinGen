package lucas.basemodel.modules.financeiro.dto;

import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContaStagingForm {
    private List<ContaStagingItem> contas;

    @Data
    public static class ContaStagingItem {
        private LocalDate dataVencimento;
        private String descricao;
        private BigDecimal valor;
        private TipoTransacao tipo;
        private Long categoriaId;
        private Frequencia frequencia;
        private boolean incluir; 
    }
}
