package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.TotaisMes;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private final DashboardQueryService consulta = mock(DashboardQueryService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContaService contaService = mock(ContaService.class);
    private final EspacoFinanceiroService espacos = mock(EspacoFinanceiroService.class);
    private final DashboardService service = new DashboardService(consulta, usuarios, contaService, espacos);
    private final User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();

    private static Map<EscopoTransacao, TotaisMes> totais(String pessoal, String casa) {
        Map<EscopoTransacao, TotaisMes> mapa = new EnumMap<>(EscopoTransacao.class);
        BigDecimal zero = BigDecimal.ZERO;
        mapa.put(EscopoTransacao.PESSOAL, new TotaisMes(zero, new BigDecimal(pessoal), zero));
        mapa.put(EscopoTransacao.CASA, new TotaisMes(zero, new BigDecimal(casa), zero));
        mapa.put(EscopoTransacao.NEGOCIO, new TotaisMes(zero, zero, zero));
        return mapa;
    }

    @Test
    void resumoPessoalNaoMisturaDespesasDaCasa() {
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(consulta.totaisDoMes(user, YearMonth.now())).thenReturn(totais("100.00", "900.00"));

        var resumo = service.getSummary(user.getEmail(), EscopoTransacao.PESSOAL);

        assertEquals(new BigDecimal("100.00"), resumo.getTotalDespesas());
        assertEquals(new BigDecimal("900.00"), resumo.getGastosCasa());
        assertEquals(EscopoTransacao.PESSOAL, resumo.getEscopo());
        verify(espacos).validarAcesso(user, EscopoTransacao.PESSOAL);
    }

    @Test
    void resumoSemContasDevolveZeros() {
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(consulta.totaisDoMes(user, YearMonth.now())).thenReturn(totais("0", "0"));

        var resumo = service.getSummary(user.getEmail(), EscopoTransacao.PESSOAL);

        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.getTotalDespesas()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.getFreeCashFlow()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.getTotalReceitas()));
    }

    @Test
    void proximasContasUsamConsultaLimitadaSemCarregarTodas() {
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(consulta.proximasPendentes(eq(user), eq(EscopoTransacao.PESSOAL), eq(null), any(LocalDate.class), eq(5)))
                .thenReturn(List.of(new Conta()));

        service.getProximasContas(user.getEmail(), EscopoTransacao.PESSOAL);

        verify(consulta).proximasPendentes(eq(user), eq(EscopoTransacao.PESSOAL), eq(null), any(LocalDate.class), eq(5));
        verify(contaService, never()).listarContasAVencer(any());
    }

    @Test
    void transacoesRecentesUsamConsultaLimitada() {
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(consulta.pagasRecentes(user, EscopoTransacao.PESSOAL, 5)).thenReturn(List.of());

        service.getTransacoesRecentes(user.getEmail(), EscopoTransacao.PESSOAL);

        verify(consulta).pagasRecentes(user, EscopoTransacao.PESSOAL, 5);
    }
}
