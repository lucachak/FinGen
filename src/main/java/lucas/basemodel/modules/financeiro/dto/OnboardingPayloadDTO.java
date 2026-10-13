package lucas.basemodel.modules.financeiro.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.enums.TransportePrincipal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnboardingPayloadDTO {
    // Step 1: Principal Income
    private BigDecimal rendaLiquida;
    private BigDecimal saldoDisponivel;
    private String nomeConta;

    // Step 2: Housing
    private SituacaoMoradia situacaoMoradia;
    private BigDecimal valorMoradia;        // Aluguel ou prestação
    private BigDecimal ajudaFamiliar;       // Valor que contribui se mora COM_OS_PAIS

    // Step 3: House Bills (skipped if COM_OS_PAIS)
    // List of identifiers like 'luz', 'agua', 'internet'
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> contasCasa = new ArrayList<>();

    // Step 4: Food Expenses
    private BigDecimal gastoAlimentacao;    // Gasto médio mensal com alimentação

    // Step 5: Transport and Lifestyle
    private TransportePrincipal transportePrincipal;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> assinaturas = new ArrayList<>(); // 'netflix', 'spotify', etc

    // Step 6: Business Profile
    private boolean possuiNegocio;          // Tem empresa / CNPJ / MEI
    private String nomeNegocio;             // Nome do negócio (opcional)

    // Legacy / Additional profile questions
    private boolean possuiDividasAtivas;
    private boolean possuiDependentes;
    private int numeroDependentes;
}
