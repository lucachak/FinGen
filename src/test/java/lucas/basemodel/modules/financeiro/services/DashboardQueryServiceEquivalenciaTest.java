package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.ResumoAtraso;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.TotaisMes;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.perf.ContaDatasetFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que as agregações em SQL do DashboardQueryService dão os mesmos números que a
 * lógica em memória usada hoje pelo DashboardController (reimplementada em ReferenciaLegada).
 */
@SpringBootTest
@Transactional
class DashboardQueryServiceEquivalenciaTest {

    @Autowired DashboardQueryService service;
    @Autowired ContaRepository contas;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;

    final LocalDate hoje = LocalDate.now();
    final YearMonth mes = YearMonth.now();
    User a;
    User b;

    @BeforeEach
    void dataset() {
        a = usuarios.save(novoUsuario("a"));
        b = usuarios.save(novoUsuario("b"));
        ContaDatasetFactory.popular(contas, categorias, a, 5_000, hoje);
        ContaDatasetFactory.popular(contas, categorias, b, 500, hoje);
    }

    private static User novoUsuario(String prefixo) {
        String id = prefixo + "-" + UUID.randomUUID();
        return User.builder().email(id + "@t.com").username(id).password("x").role("ROLE_USER").build();
    }

    /** Cópia fiel de DashboardController.calcularTotal/calcularPendente/listas, sobre a lista completa. */
    static final class ReferenciaLegada {
        final List<Conta> todas;

        ReferenciaLegada(List<Conta> todas) {
            this.todas = todas;
        }

        List<Conta> escopo(EscopoTransacao e) {
            return todas.stream().filter(c -> c.getEscopo() == e).toList();
        }

        BigDecimal total(EscopoTransacao e, TipoTransacao tipo, YearMonth m) {
            return escopo(e).stream()
                    .filter(c -> c.getTipo() == tipo)
                    .filter(Conta::isPaga)
                    .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(m))
                    .map(Conta::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal pendente(EscopoTransacao e, TipoTransacao tipo, YearMonth m) {
            return escopo(e).stream()
                    .filter(c -> c.getTipo() == tipo)
                    .filter(c -> !c.isPaga())
                    .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(m))
                    .map(Conta::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<Conta> pendentesDespesa(EscopoTransacao e) {
            return escopo(e).stream()
                    .filter(c -> c.getTipo() == TipoTransacao.DESPESA && !c.isPaga())
                    .filter(c -> c.getDataVencimento() != null).toList();
        }

        List<Conta> atrasadas(EscopoTransacao e, LocalDate h) {
            return pendentesDespesa(e).stream().filter(c -> c.getDataVencimento().isBefore(h)).toList();
        }

        List<Conta> vencendo(EscopoTransacao e, LocalDate h, int dias) {
            return pendentesDespesa(e).stream()
                    .filter(c -> !c.getDataVencimento().isBefore(h))
                    .filter(c -> !c.getDataVencimento().isAfter(h.plusDays(dias))).toList();
        }

        List<Conta> proximos(EscopoTransacao e, LocalDate h, int limite) {
            return pendentesDespesa(e).stream()
                    .filter(c -> !c.getDataVencimento().isBefore(h))
                    .sorted(Comparator.comparing(Conta::getDataVencimento))
                    .limit(limite).toList();
        }

        List<Conta> recentes(EscopoTransacao e, int limite) {
            return escopo(e).stream()
                    .filter(Conta::isPaga).filter(c -> c.getDataPagamento() != null)
                    .sorted(Comparator.comparing(Conta::getDataPagamento, Comparator.reverseOrder()))
                    .limit(limite).toList();
        }

        Map<String, BigDecimal> despesasPorCategoria(EscopoTransacao e, YearMonth m) {
            return escopo(e).stream()
                    .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                    .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(m))
                    .collect(Collectors.groupingBy(
                            c -> c.getCategoria() != null ? c.getCategoria().getNome() : "Outros",
                            Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)));
        }
    }

    private ReferenciaLegada referencia(User u) {
        return new ReferenciaLegada(contas.findAllByResponsavelOrderByDataVencimentoAsc(u));
    }

    private static void assertMesmoValor(BigDecimal esperado, BigDecimal obtido, String contexto) {
        assertEquals(0, esperado.compareTo(obtido), contexto + ": esperado " + esperado + " obtido " + obtido);
    }

    @Test
    void totaisIguaisAReferenciaPorEscopo() {
        ReferenciaLegada ref = referencia(a);
        Map<EscopoTransacao, TotaisMes> totais = service.totaisDoMes(a, mes);

        for (EscopoTransacao e : EscopoTransacao.values()) {
            TotaisMes t = totais.get(e);
            assertMesmoValor(ref.total(e, TipoTransacao.DESPESA, mes), t.despesaPaga(), "despesaPaga " + e);
            assertMesmoValor(ref.pendente(e, TipoTransacao.DESPESA, mes), t.despesaPendente(), "despesaPendente " + e);
            assertMesmoValor(ref.total(e, TipoTransacao.RECEITA, mes), t.receitaPaga(), "receitaPaga " + e);
        }
        assertTrue(totais.values().stream().anyMatch(t -> t.despesaPaga().signum() > 0),
                "dataset deveria ter despesas pagas no mês corrente");
    }

    @Test
    void atrasadasEVencendoIguaisAReferencia() {
        ReferenciaLegada ref = referencia(a);
        for (EscopoTransacao e : EscopoTransacao.values()) {
            List<Conta> atrasadas = ref.atrasadas(e, hoje);
            ResumoAtraso atraso = service.atrasadas(a, e, hoje);
            assertEquals(atrasadas.size(), atraso.quantidade(), "qtd atrasadas " + e);
            assertMesmoValor(soma(atrasadas), atraso.total(), "total atrasado " + e);

            List<Conta> vencendo = ref.vencendo(e, hoje, 7);
            ResumoAtraso venc = service.vencendo(a, e, hoje, 7);
            assertEquals(vencendo.size(), venc.quantidade(), "qtd vencendo " + e);
            assertMesmoValor(soma(vencendo), venc.total(), "total vencendo " + e);
        }
    }

    @Test
    void proximasEPagasRecentesTemAsMesmasDatas() {
        ReferenciaLegada ref = referencia(a);
        for (EscopoTransacao e : EscopoTransacao.values()) {
            assertEquals(
                    ref.proximos(e, hoje, 5).stream().map(Conta::getDataVencimento).toList(),
                    service.proximasPendentes(a, e, TipoTransacao.DESPESA, hoje, 5).stream()
                            .map(Conta::getDataVencimento).toList(),
                    "próximas " + e);
            assertEquals(
                    ref.recentes(e, 5).stream().map(Conta::getDataPagamento).toList(),
                    service.pagasRecentes(a, e, 5).stream().map(Conta::getDataPagamento).toList(),
                    "recentes " + e);
        }
    }

    @Test
    void despesasPorCategoriaIguaisAReferenciaIncluindoOutros() {
        ReferenciaLegada ref = referencia(a);
        for (EscopoTransacao e : EscopoTransacao.values()) {
            Map<String, BigDecimal> esperado = ref.despesasPorCategoria(e, mes);
            Map<String, BigDecimal> obtido = service.despesasPorCategoria(a, e, mes);
            assertEquals(esperado.keySet(), obtido.keySet(), "categorias " + e);
            esperado.forEach((k, v) -> assertMesmoValor(v, obtido.get(k), "categoria " + k + " " + e));
        }
        assertTrue(service.despesasPorCategoria(a, EscopoTransacao.PESSOAL, mes).containsKey("Outros"),
                "contas sem categoria deveriam cair em 'Outros'");
    }

    @Test
    void gastosPagosPorCategoriaIgualASomaManual() {
        List<Conta> todas = contas.findAllByResponsavelOrderByDataVencimentoAsc(a);
        Set<Long> ids = todas.stream().filter(c -> c.getCategoria() != null)
                .map(c -> c.getCategoria().getId()).collect(Collectors.toSet());
        Map<Long, BigDecimal> esperado = new HashMap<>();
        todas.stream()
                .filter(c -> c.getCategoria() != null && c.getTipo() == TipoTransacao.DESPESA && c.isPaga())
                .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(mes))
                .forEach(c -> esperado.merge(c.getCategoria().getId(), c.getValor(), BigDecimal::add));

        Map<Long, BigDecimal> obtido = service.gastosPagosPorCategoria(a, ids, mes);

        assertEquals(esperado.keySet(), obtido.keySet());
        esperado.forEach((k, v) -> assertMesmoValor(v, obtido.get(k), "categoria id " + k));
        assertTrue(service.gastosPagosPorCategoria(a, Set.of(), mes).isEmpty());
    }

    @Test
    void usuarioSemContasDevolveZerosEListasVazias() {
        User vazio = usuarios.save(novoUsuario("vazio"));

        Map<EscopoTransacao, TotaisMes> totais = service.totaisDoMes(vazio, mes);

        for (EscopoTransacao e : EscopoTransacao.values()) {
            TotaisMes t = totais.get(e);
            assertMesmoValor(BigDecimal.ZERO, t.despesaPaga(), "despesaPaga");
            assertMesmoValor(BigDecimal.ZERO, t.despesaPendente(), "despesaPendente");
            assertMesmoValor(BigDecimal.ZERO, t.receitaPaga(), "receitaPaga");
            assertEquals(0L, service.atrasadas(vazio, e, hoje).quantidade());
            assertMesmoValor(BigDecimal.ZERO, service.vencendo(vazio, e, hoje, 7).total(), "vencendo");
            assertTrue(service.proximasPendentes(vazio, e, TipoTransacao.DESPESA, hoje, 5).isEmpty());
            assertTrue(service.pagasRecentes(vazio, e, 5).isEmpty());
            assertTrue(service.despesasPorCategoria(vazio, e, mes).isEmpty());
            assertFalse(service.possuiContas(vazio, e));
        }
    }

    @Test
    void contasDeOutroUsuarioNaoEntram() {
        User unico = usuarios.save(novoUsuario("unico"));
        Conta c = new Conta();
        c.setDescricao("única");
        c.setResponsavel(unico);
        c.setEscopo(EscopoTransacao.PESSOAL);
        c.setTipo(TipoTransacao.DESPESA);
        c.setPaga(true);
        c.setValor(new BigDecimal("12.34"));
        c.setDataVencimento(hoje);
        c.setDataPagamento(hoje);
        contas.save(c);

        Map<EscopoTransacao, TotaisMes> totais = service.totaisDoMes(unico, mes);

        assertMesmoValor(new BigDecimal("12.34"), totais.get(EscopoTransacao.PESSOAL).despesaPaga(), "pessoal");
        assertMesmoValor(BigDecimal.ZERO, totais.get(EscopoTransacao.CASA).despesaPaga(), "casa");
        assertTrue(service.possuiContas(unico, EscopoTransacao.PESSOAL));
        assertFalse(service.possuiContas(unico, EscopoTransacao.NEGOCIO));
    }

    private static BigDecimal soma(List<Conta> lista) {
        return lista.stream().map(Conta::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
