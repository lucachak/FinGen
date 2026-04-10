package lucas.basemodel.modules.financeiro.dto;

import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.enums.TransportePrincipal;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OnboardingPayloadDTO {
    // Step 1: Principal Income
    private BigDecimal rendaLiquida;

    // Step 2: Housing
    private SituacaoMoradia situacaoMoradia;
    private BigDecimal valorMoradia; // Aluguel ou prestação

    // Step 3: House Bills (List of identifiers like 'luz', 'agua', 'internet')
    private List<String> contasCasa;

    // Step 4: Transport and Lifestyle
    private TransportePrincipal transportePrincipal;
    private List<String> assinaturas; // 'netflix', 'spotify', etc
    
    // Additional profile questions
    private boolean possuiDividasAtivas;
    private boolean possuiDependentes;
    private int numeroDependentes;
}
