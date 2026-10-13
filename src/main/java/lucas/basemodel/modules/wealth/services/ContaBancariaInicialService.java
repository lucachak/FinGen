package lucas.basemodel.modules.wealth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.enums.AssetType;
import lucas.basemodel.modules.wealth.enums.BankAccountType;
import lucas.basemodel.modules.wealth.models.AssetValuation;
import lucas.basemodel.modules.wealth.models.BankAccountAsset;
import lucas.basemodel.modules.wealth.repositories.AssetValuationRepository;
import lucas.basemodel.modules.wealth.repositories.BankAccountAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContaBancariaInicialService {

    private static final String NOME_PADRAO = "Conta principal";

    private final BankAccountAssetRepository bankAccountRepository;
    private final AssetValuationRepository valuationRepository;

    /**
     * Garante uma conta transacional para o usuário sem duplicá-la em tentativas
     * repetidas do onboarding. O saldo pode ser positivo, zero ou negativo.
     */
    @Transactional
    public BankAccountAsset criarSeAusente(User user, BigDecimal saldoDisponivel, String nomeInformado) {
        Objects.requireNonNull(user, "Usuário é obrigatório para criar a conta inicial");

        return bankAccountRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                .orElseGet(() -> criar(user, saldoDisponivel, nomeInformado));
    }

    private BankAccountAsset criar(User user, BigDecimal saldoDisponivel, String nomeInformado) {
        BigDecimal saldoInicial = saldoDisponivel != null ? saldoDisponivel : BigDecimal.ZERO;
        String nome = nomeInformado != null && !nomeInformado.isBlank()
                ? nomeInformado.trim()
                : NOME_PADRAO;
        LocalDateTime agora = LocalDateTime.now();

        BankAccountAsset conta = BankAccountAsset.builder()
                .user(user)
                .name(nome)
                .description("Conta criada durante o onboarding")
                .type(AssetType.BANK_ACCOUNT)
                .estimatedValue(saldoInicial)
                .lastValuationAt(agora)
                .bankName(nome)
                .accountType(BankAccountType.CHECKING)
                .balance(saldoInicial)
                .currency("BRL")
                .build();

        BankAccountAsset salva = bankAccountRepository.save(conta);
        valuationRepository.save(AssetValuation.builder()
                .asset(salva)
                .amount(saldoInicial)
                .currency("BRL")
                .source("ONBOARDING_INITIAL_BALANCE")
                .build());

        log.info("Conta bancária inicial {} criada para o usuário {}", salva.getId(), user.getId());
        return salva;
    }
}
