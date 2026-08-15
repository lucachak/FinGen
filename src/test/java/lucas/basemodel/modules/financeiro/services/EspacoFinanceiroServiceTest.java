package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.user.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EspacoFinanceiroServiceTest {

    private final ConfiguracaoFinanceiraRepository repository = mock(ConfiguracaoFinanceiraRepository.class);
    private final EspacoFinanceiroService service = new EspacoFinanceiroService(repository);
    private final User user = User.builder().id(UUID.randomUUID()).build();

    @Test
    void bloqueiaNegocioQuandoPerfilNaoPossuiEmpresa() {
        when(repository.findByUser(user)).thenReturn(Optional.of(
                ConfiguracaoFinanceira.builder().user(user).possuiNegocio(false).build()));

        assertThrows(BadRequestException.class,
                () -> service.validarAcesso(user, EscopoTransacao.NEGOCIO));
        assertTrue(service.listarPermitidos(user).contains(EscopoTransacao.PESSOAL));
    }

    @Test
    void bloqueiaCasaParaQuemDeclarouMorarComOsPais() {
        when(repository.findByUser(user)).thenReturn(Optional.of(
                ConfiguracaoFinanceira.builder().user(user)
                        .situacaoMoradia(SituacaoMoradia.COM_OS_PAIS).build()));

        assertThrows(BadRequestException.class,
                () -> service.validarAcesso(user, EscopoTransacao.CASA));
    }
}
