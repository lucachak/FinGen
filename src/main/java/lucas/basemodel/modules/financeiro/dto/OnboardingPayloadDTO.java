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
    private BigDecimal valorMoradia;        // Aluguel ou prestação
    private BigDecimal ajudaFamiliar;       // Valor que contribui se mora COM_OS_PAIS

    // Step 3: House Bills (skipped if COM_OS_PAIS)
    // List of identifiers like 'luz', 'agua', 'internet'
    private List<String> contasCasa;

    // Step 4: Food Expenses
    private BigDecimal gastoAlimentacao;    // Gasto médio mensal com alimentação

    // Step 5: Transport and Lifestyle
    private TransportePrincipal transportePrincipal;
    private List<String> assinaturas;       // 'netflix', 'spotify', etc

    // Step 6: Business Profile
    private boolean possuiNegocio;          // Tem empresa / CNPJ / MEI
    private String nomeNegocio;             // Nome do negócio (opcional)

    // Legacy / Additional profile questions
    private boolean possuiDividasAtivas;
    private boolean possuiDependentes;
    private int numeroDependentes;
}
