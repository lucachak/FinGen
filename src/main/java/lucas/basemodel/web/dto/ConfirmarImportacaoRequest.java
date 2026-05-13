package lucas.basemodel.web.dto;

import lombok.Data;
import lucas.basemodel.modules.financeiro.dto.ContaRequest;

import java.util.List;

@Data
public class ConfirmarImportacaoRequest {
    private String sessionId;
    private List<ContaRequest> transacoes;
}
