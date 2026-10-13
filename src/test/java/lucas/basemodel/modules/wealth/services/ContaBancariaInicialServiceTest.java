package lucas.basemodel.modules.wealth.services;

import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.enums.AssetType;
import lucas.basemodel.modules.wealth.models.AssetValuation;
import lucas.basemodel.modules.wealth.models.BankAccountAsset;
import lucas.basemodel.modules.wealth.repositories.AssetValuationRepository;
import lucas.basemodel.modules.wealth.repositories.BankAccountAssetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContaBancariaInicialServiceTest {

    @Test
    void criaContaComUuidSaldoNegativoEPrimeiroRegistro() {
        BankAccountAssetRepository contas = mock(BankAccountAssetRepository.class);
        AssetValuationRepository avaliacoes = mock(AssetValuationRepository.class);
        ContaBancariaInicialService service = new ContaBancariaInicialService(contas, avaliacoes);
        User user = User.builder().id(UUID.randomUUID()).build();

        when(contas.findFirstByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(Optional.empty());
        when(contas.save(any(BankAccountAsset.class))).thenAnswer(invocation -> {
            BankAccountAsset conta = invocation.getArgument(0);
            conta.setId(UUID.randomUUID());
            return conta;
        });

        BankAccountAsset criada = service.criarSeAusente(user, new BigDecimal("-480.25"), "Conta corrente");

        assertEquals(AssetType.BANK_ACCOUNT, criada.getType());
        assertEquals(new BigDecimal("-480.25"), criada.getBalance());
        assertEquals(criada.getBalance(), criada.getEstimatedValue());
        ArgumentCaptor<AssetValuation> captor = ArgumentCaptor.forClass(AssetValuation.class);
        verify(avaliacoes).save(captor.capture());
        assertSame(criada, captor.getValue().getAsset());
        assertEquals("ONBOARDING_INITIAL_BALANCE", captor.getValue().getSource());
        assertEquals(new BigDecimal("-480.25"), captor.getValue().getAmount());
    }

    @Test
    void reutilizaContaExistenteSemSobrescreverSaldo() {
        BankAccountAssetRepository contas = mock(BankAccountAssetRepository.class);
        AssetValuationRepository avaliacoes = mock(AssetValuationRepository.class);
        ContaBancariaInicialService service = new ContaBancariaInicialService(contas, avaliacoes);
        User user = User.builder().id(UUID.randomUUID()).build();
        BankAccountAsset existente = BankAccountAsset.builder()
                .id(UUID.randomUUID())
                .user(user)
                .balance(new BigDecimal("900.00"))
                .build();
        when(contas.findFirstByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(Optional.of(existente));

        BankAccountAsset resultado = service.criarSeAusente(user, BigDecimal.ZERO, "Outra");

        assertSame(existente, resultado);
        assertEquals(new BigDecimal("900.00"), resultado.getBalance());
        verify(contas, never()).save(any());
        verifyNoInteractions(avaliacoes);
    }
}
