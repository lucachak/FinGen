package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.dto.ResumoPendente;
import lucas.basemodel.modules.financeiro.dto.TotalMesPorEscopo;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ContaRepositoryAgregacaoTest {

    @Autowired ContaRepository contas;
    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;

    User a;
    User b;

    @BeforeEach
    void usuarios() {
        a = usuarios.save(novoUsuario("a"));
        b = usuarios.save(novoUsuario("b"));
    }

    private static User novoUsuario(String prefixo) {
        String id = prefixo + "-" + UUID.randomUUID();
        return User.builder().email(id + "@t.com").username(id).password("x").role("ROLE_USER").build();
    }

    private Conta conta(User dono, EscopoTransacao escopo, TipoTransacao tipo, boolean paga, String valor,
                        LocalDate vencimento, LocalDate pagamento, Categoria categoria) {
        Conta c = new Conta();
        c.setDescricao("t");
        c.setResponsavel(dono);
        c.setEscopo(escopo);
        c.setTipo(tipo);
        c.setPaga(paga);
        c.setValor(new BigDecimal(valor));
        c.setDataVencimento(vencimento);
        c.setDataPagamento(pagamento);
        c.setCategoria(categoria);
        return contas.save(c);
    }

    private Conta despesaPaga(User dono, String valor, LocalDate pagamento) {
        return conta(dono, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true, valor, pagamento, pagamento, null);
    }

    private static BigDecimal total(List<TotalMesPorEscopo> linhas, EscopoTransacao escopo, TipoTransacao tipo,
                                    boolean paga) {
        return linhas.stream()
                .filter(l -> l.escopo() == escopo && l.tipo() == tipo && l.paga() == paga)
                .map(TotalMesPorEscopo::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void somarMesIgnoraPagaSemDataPagamento() {
        LocalDate fev = LocalDate.of(2026, 2, 10);
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true, "50.00", fev, null, null);
        despesaPaga(a, "20.00", fev);

        var linhas = contas.somarMes(a, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));

        assertEquals(0, new BigDecimal("20.00")
                .compareTo(total(linhas, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true)));
    }

    @Test
    void somarMesIncluiPrimeiroEUltimoDiaEExcluiMesVizinho() {
        despesaPaga(a, "1", LocalDate.of(2026, 1, 31));
        despesaPaga(a, "2", LocalDate.of(2026, 2, 1));
        despesaPaga(a, "3", LocalDate.of(2026, 2, 28));
        despesaPaga(a, "4", LocalDate.of(2026, 3, 1));

        var linhas = contas.somarMes(a, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));

        assertEquals(0, new BigDecimal("5")
                .compareTo(total(linhas, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true)));
    }

    @Test
    void somarMesVirandoAno() {
        despesaPaga(a, "1", LocalDate.of(2026, 11, 30));
        despesaPaga(a, "2", LocalDate.of(2026, 12, 1));
        despesaPaga(a, "4", LocalDate.of(2026, 12, 31));
        despesaPaga(a, "8", LocalDate.of(2027, 1, 1));

        var linhas = contas.somarMes(a, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1));

        assertEquals(0, new BigDecimal("6")
                .compareTo(total(linhas, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true)));
    }

    @Test
    void somarMesUsaVencimentoParaPendentesENaoMisturaResponsaveis() {
        LocalDate dia = LocalDate.of(2026, 2, 15);
        conta(a, EscopoTransacao.CASA, TipoTransacao.DESPESA, false, "70.00", dia, null, null);
        conta(b, EscopoTransacao.CASA, TipoTransacao.DESPESA, false, "999.00", dia, null, null);

        var linhas = contas.somarMes(a, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));

        assertEquals(0, new BigDecimal("70.00")
                .compareTo(total(linhas, EscopoTransacao.CASA, TipoTransacao.DESPESA, false)));
    }

    @Test
    void resumirPendentesDevolveZeroSemContas() {
        ResumoPendente r = contas.resumirPendentes(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA,
                LocalDate.of(1900, 1, 1), LocalDate.of(2026, 1, 1));

        assertEquals(0L, r.quantidade());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.total()));
    }

    @Test
    void proximasPendentesRespeitaLimiteEOrdem() {
        LocalDate hoje = LocalDate.of(2026, 6, 10);
        int[] dias = {3, 1, 2, 10, 5};
        for (int d : dias) {
            conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, false, "10", hoje.plusDays(d), null, null);
        }
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.RECEITA, false, "10", hoje.plusDays(4), null, null);
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, false, "10", hoje.minusDays(1), null, null);

        List<Conta> soDespesas = contas.proximasPendentes(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA,
                hoje, PageRequest.of(0, 3));
        List<Conta> qualquerTipo = contas.proximasPendentes(a, EscopoTransacao.PESSOAL, null, hoje,
                PageRequest.of(0, 10));

        assertEquals(List.of(hoje.plusDays(1), hoje.plusDays(2), hoje.plusDays(3)),
                soDespesas.stream().map(Conta::getDataVencimento).toList());
        assertEquals(6, qualquerTipo.size());
        assertTrue(qualquerTipo.stream().noneMatch(c -> c.getDataVencimento().isBefore(hoje)));
    }

    @Test
    void pagasRecentesRespeitaLimiteEOrdem() {
        LocalDate base = LocalDate.of(2026, 6, 10);
        for (int d : new int[]{0, 3, 1, 2}) {
            despesaPaga(a, "10", base.minusDays(d));
        }
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true, "10", base, null, null); // paga sem data
        conta(a, EscopoTransacao.CASA, TipoTransacao.DESPESA, true, "10", base, base, null);    // outro escopo

        List<Conta> recentes = contas.pagasRecentes(a, EscopoTransacao.PESSOAL, PageRequest.of(0, 3));

        assertEquals(List.of(base, base.minusDays(1), base.minusDays(2)),
                recentes.stream().map(Conta::getDataPagamento).toList());
    }

    @Test
    void somarDespesasPorCategoriaAgrupaSemCategoriaComoNula() {
        Categoria cat = new Categoria();
        cat.setNome("Agreg-" + UUID.randomUUID());
        cat.setEscopo(EscopoTransacao.PESSOAL);
        cat.setCorHexadecimal("#112233");
        cat = categorias.save(cat);
        LocalDate dia = LocalDate.of(2026, 2, 10);
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, false, "30.00", dia, null, cat);
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, true, "20.00", dia, dia, null);
        conta(a, EscopoTransacao.PESSOAL, TipoTransacao.RECEITA, true, "500.00", dia, dia, null);

        List<Object[]> linhas = contas.somarDespesasPorCategoria(a, EscopoTransacao.PESSOAL,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));

        assertEquals(2, linhas.size());
        Object[] semCategoria = linhas.stream().filter(l -> l[0] == null).findFirst().orElseThrow();
        assertNull(semCategoria[1]);
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) semCategoria[2]));
        Object[] comCategoria = linhas.stream().filter(l -> l[0] != null).findFirst().orElseThrow();
        assertEquals(cat.getNome(), comCategoria[0]);
        assertEquals(0, new BigDecimal("30.00").compareTo((BigDecimal) comCategoria[2]));
    }

    @Test
    void existeContaPorEscopoSoParaOResponsavel() {
        despesaPaga(a, "1", LocalDate.of(2026, 2, 1));

        assertTrue(contas.existsByResponsavelAndEscopo(a, EscopoTransacao.PESSOAL));
        assertEquals(false, contas.existsByResponsavelAndEscopo(a, EscopoTransacao.NEGOCIO));
        assertEquals(false, contas.existsByResponsavelAndEscopo(b, EscopoTransacao.PESSOAL));
    }
}
