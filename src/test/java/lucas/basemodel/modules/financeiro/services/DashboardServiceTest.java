package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    @Test
    void resumoPessoalNaoMisturaDespesasDaCasa() {
        ContaRepository contas = mock(ContaRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ContaService contaService = mock(ContaService.class);
        EspacoFinanceiroService espacos = mock(EspacoFinanceiroService.class);
        DashboardService service = new DashboardService(contas, usuarios, contaService, espacos);
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();

        Conta pessoal = conta(EscopoTransacao.PESSOAL, "100.00");
        Conta casa = conta(EscopoTransacao.CASA, "900.00");
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(contas.findAllByResponsavelOrderByDataVencimentoAsc(user)).thenReturn(List.of(pessoal, casa));

        var resumo = service.getSummary(user.getEmail(), EscopoTransacao.PESSOAL);

        assertEquals(new BigDecimal("100.00"), resumo.getTotalDespesas());
        assertEquals(new BigDecimal("900.00"), resumo.getGastosCasa());
        assertEquals(EscopoTransacao.PESSOAL, resumo.getEscopo());
        verify(espacos).validarAcesso(user, EscopoTransacao.PESSOAL);
    }

    private Conta conta(EscopoTransacao escopo, String valor) {
        Conta conta = new Conta();
        conta.setEscopo(escopo);
        conta.setTipo(TipoTransacao.DESPESA);
        conta.setValor(new BigDecimal(valor));
        conta.setPaga(true);
        conta.setDataPagamento(LocalDate.now());
        conta.setDataVencimento(LocalDate.now());
        return conta;
    }
}
