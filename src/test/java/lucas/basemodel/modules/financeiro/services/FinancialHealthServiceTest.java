package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.services.DashboardQueryService.ResumoAtraso;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.TotaisMes;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.services.WealthService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Valores esperados calculados à mão a partir da fórmula que já existia
 * (comprometimento > 70% penaliza, atraso penaliza 15 + agravante até 20, poupança > 20% dá bônus até 15).
 */
class FinancialHealthServiceTest {

    private final WealthService wealth = mock(WealthService.class);
    private final FinancialHealthService service = new FinancialHealthService(wealth);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static void assertValor(String esperado, BigDecimal obtido, String contexto) {
        assertEquals(0, bd(esperado).compareTo(obtido), contexto + ": esperado " + esperado + " obtido " + obtido);
    }

    private User usuario(BigDecimal orcamentoMensal) {
        User u = User.builder().email("h@t.com").username("h").password("x").build();
        u.setOrcamentoMensal(orcamentoMensal);
        return u;
    }

    @Test
    void semReceitaUsaOrcamentoMensalComoRendaEstimada() {
        when(wealth.getMonthlyIncomeAssets(org.mockito.ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);

        var r = service.calcular(usuario(bd("3500.00")),
                new TotaisMes(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new ResumoAtraso(0, BigDecimal.ZERO), List.of());

        assertValor("100.00", r.taxaPoupancaEfetiva(), "taxa");
        assertValor("0", r.comprometimentoRenda(), "comprometimento");
        assertEquals(100, r.scoreSaude());
        assertEquals("Excelente", r.classificacao());
        assertEquals(3, r.projecao3Meses().size());
    }

    @Test
    void comprometimentoAltoComAtrasoPenalizaScore() {
        when(wealth.getMonthlyIncomeAssets(org.mockito.ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);

        // receita 1000, gasto 700 + 150 = 850 (85%), poupança 15%, atraso de 2 contas somando 100
        var r = service.calcular(usuario(bd("3500.00")),
                new TotaisMes(bd("1000"), bd("700"), bd("150")),
                new ResumoAtraso(2, bd("100")), List.of());

        assertValor("15.00", r.taxaPoupancaEfetiva(), "taxa");
        assertValor("85.00", r.comprometimentoRenda(), "comprometimento");
        assertEquals(60, r.scoreSaude()); // 100 - 15 (acima de 70%) - 15 (atraso) - 10 (agravante 100/1000)
        assertEquals("Saudável", r.classificacao());
    }

    @Test
    void poupancaAltaDaBonusELimitaEm100() {
        when(wealth.getMonthlyIncomeAssets(org.mockito.ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);

        var r = service.calcular(usuario(bd("3500.00")),
                new TotaisMes(bd("2000"), bd("800"), bd("200")),
                new ResumoAtraso(0, BigDecimal.ZERO), List.of());

        assertValor("50.00", r.taxaPoupancaEfetiva(), "taxa");
        assertValor("50.00", r.comprometimentoRenda(), "comprometimento");
        assertEquals(100, r.scoreSaude());
        assertEquals("Excelente", r.classificacao());
    }

    @Test
    void semReceitaENemOrcamentoNaoDivideporZero() {
        when(wealth.getMonthlyIncomeAssets(org.mockito.ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);

        var r = service.calcular(usuario(null),
                new TotaisMes(BigDecimal.ZERO, bd("50"), BigDecimal.ZERO),
                new ResumoAtraso(1, bd("30")), List.of());

        assertValor("0", r.taxaPoupancaEfetiva(), "taxa");
        assertValor("0", r.comprometimentoRenda(), "comprometimento");
        assertEquals(85, r.scoreSaude()); // 100 - 15 (atraso); sem receita não há agravante nem bônus
    }
}
