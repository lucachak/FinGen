package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrcamentoServiceBatchTest {

    private final OrcamentoRepository orcamentos = mock(OrcamentoRepository.class);
    private final ContaRepository contas = mock(ContaRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CategoriaService categoriaService = mock(CategoriaService.class);
    private final EspacoFinanceiroService espacos = mock(EspacoFinanceiroService.class);
    private final DashboardQueryService consulta = mock(DashboardQueryService.class);
    private final OrcamentoService service = new OrcamentoService(
            orcamentos, contas, categorias, usuarios, categoriaService, espacos, consulta);

    private static Orcamento orcamento(long categoriaId, String limite) {
        Categoria c = new Categoria();
        c.setId(categoriaId);
        c.setNome("Cat " + categoriaId);
        c.setEscopo(EscopoTransacao.PESSOAL);
        return Orcamento.builder().id(UUID.randomUUID()).categoria(c).limiteMensal(new BigDecimal(limite)).build();
    }

    @Test
    void calculaGastosDeVariasCategoriasComUmaConsultaDeSoma() {
        User user = User.builder().id(UUID.randomUUID()).email("o@t.com").build();
        when(usuarios.findByEmail("o@t.com")).thenReturn(Optional.of(user));
        when(orcamentos.findByResponsavel(user)).thenReturn(List.of(
                orcamento(1, "100"), orcamento(2, "200"), orcamento(3, "300")));
        when(consulta.gastosPagosPorCategoria(eq(user), anyCollection(), eq(YearMonth.now())))
                .thenReturn(Map.of(1L, new BigDecimal("50"), 2L, new BigDecimal("150")));

        var respostas = service.listarComConsumo("o@t.com", EscopoTransacao.PESSOAL);

        assertEquals(Set.of(new BigDecimal("50"), new BigDecimal("150"), BigDecimal.ZERO),
                respostas.stream().map(r -> r.getGastoAtual()).collect(Collectors.toSet()));
        verify(consulta, times(1)).gastosPagosPorCategoria(eq(user), anyCollection(), any(YearMonth.class));
        verify(contas, never()).sumGastosPorCategoriaMesAno(any(), any(), any(), any());
    }

    @Test
    void semOrcamentosNaoConsultaGastos() {
        User user = User.builder().id(UUID.randomUUID()).email("o@t.com").build();
        when(usuarios.findByEmail("o@t.com")).thenReturn(Optional.of(user));
        when(orcamentos.findByResponsavel(user)).thenReturn(List.of());

        var respostas = service.listarComConsumo("o@t.com", EscopoTransacao.PESSOAL);

        assertEquals(0, respostas.size());
        verify(contas, never()).sumGastosPorCategoriaMesAno(any(), any(), any(), any());
    }
}
