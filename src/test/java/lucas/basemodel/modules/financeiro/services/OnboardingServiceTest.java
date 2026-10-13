package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.dto.OnboardingPayloadDTO;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.TransacaoRecorrenteRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.services.ContaBancariaInicialService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {

    @Test
    void concluiOnboardingSemDespesasOpcionais() {
        ConfiguracaoFinanceiraRepository configuracoes = mock(ConfiguracaoFinanceiraRepository.class);
        TransacaoRecorrenteRepository recorrencias = mock(TransacaoRecorrenteRepository.class);
        DistribuicaoOrcamentoService distribuicao = mock(DistribuicaoOrcamentoService.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ContaBancariaInicialService contaInicial = mock(ContaBancariaInicialService.class);
        OnboardingService service = new OnboardingService(configuracoes, recorrencias, distribuicao, usuarios, contaInicial);

        User user = User.builder().email("novo@example.com").build();
        OnboardingPayloadDTO payload = new OnboardingPayloadDTO();
        payload.setRendaLiquida(BigDecimal.ZERO);
        payload.setSituacaoMoradia(SituacaoMoradia.QUITADO);
        when(configuracoes.findByUser(user)).thenReturn(Optional.empty());

        service.processarOnboarding(user, payload);

        assertTrue(user.isSetupCompleted());
        verify(configuracoes).save(any(ConfiguracaoFinanceira.class));
        verify(recorrencias, never()).saveAll(any());
        verifyNoInteractions(distribuicao);
        verify(contaInicial).criarSeAusente(user, null, null);
        verify(usuarios).save(user);
    }

    @Test
    void preservaOnboardingQuandoDistribuicaoAutomaticaFalha() {
        ConfiguracaoFinanceiraRepository configuracoes = mock(ConfiguracaoFinanceiraRepository.class);
        TransacaoRecorrenteRepository recorrencias = mock(TransacaoRecorrenteRepository.class);
        DistribuicaoOrcamentoService distribuicao = mock(DistribuicaoOrcamentoService.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ContaBancariaInicialService contaInicial = mock(ContaBancariaInicialService.class);
        OnboardingService service = new OnboardingService(configuracoes, recorrencias, distribuicao, usuarios, contaInicial);

        User user = User.builder().email("novo@example.com").build();
        OnboardingPayloadDTO payload = new OnboardingPayloadDTO();
        payload.setRendaLiquida(new BigDecimal("3000"));
        payload.setSituacaoMoradia(SituacaoMoradia.QUITADO);
        when(configuracoes.findByUser(user)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("falha opcional"))
                .when(distribuicao).aplicarEstrategia(any(), any(), any());

        assertDoesNotThrow(() -> service.processarOnboarding(user, payload));

        assertTrue(user.isSetupCompleted());
        verify(usuarios).save(user);
    }

    @Test
    void repassaSaldoNegativoParaContaInicial() {
        ConfiguracaoFinanceiraRepository configuracoes = mock(ConfiguracaoFinanceiraRepository.class);
        TransacaoRecorrenteRepository recorrencias = mock(TransacaoRecorrenteRepository.class);
        DistribuicaoOrcamentoService distribuicao = mock(DistribuicaoOrcamentoService.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ContaBancariaInicialService contaInicial = mock(ContaBancariaInicialService.class);
        OnboardingService service = new OnboardingService(configuracoes, recorrencias, distribuicao, usuarios, contaInicial);

        User user = User.builder().email("negativo@example.com").build();
        OnboardingPayloadDTO payload = new OnboardingPayloadDTO();
        payload.setSituacaoMoradia(SituacaoMoradia.QUITADO);
        payload.setSaldoDisponivel(new BigDecimal("-325.40"));
        payload.setNomeConta("Minha conta");
        when(configuracoes.findByUser(user)).thenReturn(Optional.empty());

        service.processarOnboarding(user, payload);

        verify(contaInicial).criarSeAusente(user, new BigDecimal("-325.40"), "Minha conta");
    }
}
