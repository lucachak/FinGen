package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.services.WealthService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por calcular métricas rigorosas de saúde financeira
 * baseadas em fluxo de caixa, inadimplência e poupança efetiva.
 */
@Service
@RequiredArgsConstructor
public class FinancialHealthService {

    private final WealthService wealthService;

    public record HealthReport(
            int scoreSaude,
            BigDecimal taxaPoupancaEfetiva,
            BigDecimal comprometimentoRenda,
            String classificacao,
            List<Map<String, Object>> projecao3Meses
    ) {}

    public HealthReport calcular(User user, List<Conta> todasContas, List<Orcamento> orcamentos) {
        YearMonth mesAtual = YearMonth.now();

        // 1. Receita Base (Patrimônio de Renda + Receitas do Mês)
        BigDecimal receitaWealth = wealthService.getMonthlyIncomeAssets(user);
        BigDecimal receitaMes = todasContas.stream()
                .filter(c -> c.getTipo() == TipoTransacao.RECEITA && c.isPaga())
                .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(mesAtual))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receitaTotal = receitaWealth.add(receitaMes);
        if (receitaTotal.compareTo(BigDecimal.ZERO) == 0 && user.getOrcamentoMensal() != null) {
            // Fallback para renda estimada se não houver dados de transação ainda
            receitaTotal = user.getOrcamentoMensal();
        }

        // 2. Despesas
        BigDecimal despesaMêsRealizada = todasContas.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA && c.isPaga())
                .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(mesAtual))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPendentesMes = todasContas.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA && !c.isPaga())
                .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(mesAtual))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Conta> contasAtrasadas = todasContas.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA && !c.isPaga())
                .filter(c -> c.getDataVencimento() != null && c.getDataVencimento().isBefore(LocalDate.now()))
                .toList();
        
        BigDecimal totalAtrasado = contasAtrasadas.stream()
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Indicadores Base
        BigDecimal taxaPoupanca = BigDecimal.ZERO;
        BigDecimal comprometimento = BigDecimal.ZERO;
        
        if (receitaTotal.compareTo(BigDecimal.ZERO) > 0) {
            // Poupança = Receita - (Despesa Paga + Despesa Pendente do Mês)
            BigDecimal poupancaMês = receitaTotal.subtract(despesaMêsRealizada).subtract(despesasPendentesMes);
            taxaPoupanca = poupancaMês.multiply(new BigDecimal("100")).divide(receitaTotal, 2, RoundingMode.HALF_UP);
            
            comprometimento = despesaMêsRealizada.add(despesasPendentesMes)
                    .multiply(new BigDecimal("100")).divide(receitaTotal, 2, RoundingMode.HALF_UP);
        }

        // 4. Cálculo do Score (0 a 100)
        int score = 100;
        
        // Penalidade A: Comprometimento de Renda > 70%
        if (comprometimento.compareTo(new BigDecimal("70")) > 0) {
            score -= (comprometimento.intValue() - 70); // Ex: 85% = -15 pts
        }
        
        // Penalidade B: Contas Atrasadas
        if (!contasAtrasadas.isEmpty()) {
            score -= 15; // Penalidade fixa
            // E agrava pelo valor atrasado vs receita
            if (receitaTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal agravante = totalAtrasado.multiply(new BigDecimal("100")).divide(receitaTotal, 0, RoundingMode.HALF_UP);
                score -= Math.min(agravante.intValue(), 20); // Max 20 pts de agravante
            }
        }
        
        // Penalidade C: Orçamentos Estourados
        int orcamentosEstourados = 0;
        // Simplificação: conta apenas a existência (ideal seria integrar o map do controller aqui depois)
        // ... (por enquanto focado em fluxo)

        // Bônus: Taxa de poupança alta (> 20%)
        if (taxaPoupanca.compareTo(new BigDecimal("20")) > 0) {
            score += Math.min(taxaPoupanca.intValue() - 20, 15);
        }

        // Limita o score entre 0 e 100
        score = Math.max(0, Math.min(100, score));

        // 5. Classificação
        String classificacao;
        if (score >= 80) classificacao = "Excelente";
        else if (score >= 60) classificacao = "Saudável";
        else if (score >= 40) classificacao = "Atenção";
        else classificacao = "Crítico";

        // 6. Projeção de 3 Meses
        List<Map<String, Object>> projecao = gerarProjecao3Meses(receitaTotal, despesaMêsRealizada.add(despesasPendentesMes), orcamentos);

        return new HealthReport(score, taxaPoupanca, comprometimento, classificacao, projecao);
    }

    private List<Map<String, Object>> gerarProjecao3Meses(BigDecimal receitaMedia, BigDecimal despesaMediaFixa, List<Orcamento> orcamentos) {
        List<Map<String, Object>> projecao = new ArrayList<>();
        YearMonth mesProjecao = YearMonth.now();

        BigDecimal totalOrcamentos = orcamentos.stream()
                .map(o -> o.getLimiteMensal() != null ? o.getLimiteMensal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A despesa projetada é a maior entre a despesa fixa mapeada e os orçamentos definidos
        BigDecimal despesaProjetada = despesaMediaFixa.max(totalOrcamentos);

        for (int i = 1; i <= 3; i++) {
            mesProjecao = mesProjecao.plusMonths(1);
            Map<String, Object> mes = new HashMap<>();
            
            String nomeMes = LocalDate.of(mesProjecao.getYear(), mesProjecao.getMonth(), 1)
                    .getMonth().getDisplayName(java.time.format.TextStyle.SHORT, new java.util.Locale("pt", "BR"));
            nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);

            BigDecimal saldo = receitaMedia.subtract(despesaProjetada);
            String risco = saldo.compareTo(BigDecimal.ZERO) < 0 ? "CRÍTICO" : (saldo.compareTo(receitaMedia.multiply(new BigDecimal("0.1"))) < 0 ? "ATENÇÃO" : "SEGURO");

            mes.put("mes", nomeMes);
            mes.put("receita", receitaMedia);
            mes.put("despesa", despesaProjetada);
            mes.put("saldo", saldo);
            mes.put("risco", risco);
            
            projecao.add(mes);
            
            // Inflacionar a despesa projetada em 1% ao mês por segurança
            despesaProjetada = despesaProjetada.multiply(new BigDecimal("1.01"));
        }
        return projecao;
    }
}
