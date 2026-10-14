package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.ResumoPendente;
import lucas.basemodel.modules.financeiro.dto.TotalMesPorEscopo;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Única fonte dos números do dashboard. Agrega no banco (intervalos de data, SUM/GROUP BY,
 * LIMIT) em vez de carregar todas as contas do usuário e somar em memória.
 *
 * Regra de data: contas pagas entram pela data de pagamento; pendentes, pela de vencimento.
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private static final LocalDate INICIO_DOS_TEMPOS = LocalDate.of(1900, 1, 1);
    private static final String SEM_CATEGORIA = "Outros";

    private final ContaRepository contaRepository;

    public record TotaisMes(BigDecimal receitaPaga, BigDecimal despesaPaga, BigDecimal despesaPendente) {
        static TotaisMes zerado() {
            return new TotaisMes(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record ResumoAtraso(long quantidade, BigDecimal total) {
    }

    /** Totais do mês por escopo; sempre traz os três escopos, com zeros quando não há contas. */
    public Map<EscopoTransacao, TotaisMes> totaisDoMes(User user, YearMonth mes) {
        Map<EscopoTransacao, BigDecimal> receitaPaga = new EnumMap<>(EscopoTransacao.class);
        Map<EscopoTransacao, BigDecimal> despesaPaga = new EnumMap<>(EscopoTransacao.class);
        Map<EscopoTransacao, BigDecimal> despesaPendente = new EnumMap<>(EscopoTransacao.class);

        for (TotalMesPorEscopo linha : contaRepository.somarMes(user, mes.atDay(1), mes.plusMonths(1).atDay(1))) {
            if (linha.tipo() == TipoTransacao.RECEITA && Boolean.TRUE.equals(linha.paga())) {
                receitaPaga.merge(linha.escopo(), linha.total(), BigDecimal::add);
            } else if (linha.tipo() == TipoTransacao.DESPESA && Boolean.TRUE.equals(linha.paga())) {
                despesaPaga.merge(linha.escopo(), linha.total(), BigDecimal::add);
            } else if (linha.tipo() == TipoTransacao.DESPESA) {
                despesaPendente.merge(linha.escopo(), linha.total(), BigDecimal::add);
            }
        }

        Map<EscopoTransacao, TotaisMes> resultado = new EnumMap<>(EscopoTransacao.class);
        for (EscopoTransacao escopo : EscopoTransacao.values()) {
            resultado.put(escopo, new TotaisMes(
                    receitaPaga.getOrDefault(escopo, BigDecimal.ZERO),
                    despesaPaga.getOrDefault(escopo, BigDecimal.ZERO),
                    despesaPendente.getOrDefault(escopo, BigDecimal.ZERO)));
        }
        return resultado;
    }

    /** Despesas pendentes com vencimento antes de {@code hoje}. */
    public ResumoAtraso atrasadas(User user, EscopoTransacao escopo, LocalDate hoje) {
        return paraAtraso(contaRepository.resumirPendentes(
                user, escopo, TipoTransacao.DESPESA, INICIO_DOS_TEMPOS, hoje));
    }

    /** Despesas pendentes vencendo de hoje até hoje + {@code dias}, inclusive. */
    public ResumoAtraso vencendo(User user, EscopoTransacao escopo, LocalDate hoje, int dias) {
        return paraAtraso(contaRepository.resumirPendentes(
                user, escopo, TipoTransacao.DESPESA, hoje, hoje.plusDays(dias + 1L)));
    }

    /** Próximas pendentes a partir de {@code hoje}; {@code tipoOuNull == null} traz qualquer tipo. */
    public List<Conta> proximasPendentes(User user, EscopoTransacao escopo, TipoTransacao tipoOuNull,
                                         LocalDate hoje, int limite) {
        return contaRepository.proximasPendentes(user, escopo, tipoOuNull, hoje, PageRequest.of(0, limite));
    }

    public List<Conta> pagasRecentes(User user, EscopoTransacao escopo, int limite) {
        return contaRepository.pagasRecentes(user, escopo, PageRequest.of(0, limite));
    }

    /** Despesas do mês (por vencimento) somadas por nome de categoria; sem categoria = "Outros". */
    public Map<String, BigDecimal> despesasPorCategoria(User user, EscopoTransacao escopo, YearMonth mes) {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        for (Object[] linha : contaRepository.somarDespesasPorCategoria(
                user, escopo, mes.atDay(1), mes.plusMonths(1).atDay(1))) {
            String nome = linha[0] != null ? (String) linha[0] : SEM_CATEGORIA;
            resultado.merge(nome, paraBigDecimal(linha[2]), BigDecimal::add);
        }
        return resultado;
    }

    /** Despesas pagas no mês (por pagamento) por id de categoria. */
    public Map<Long, BigDecimal> gastosPagosPorCategoria(User user, Collection<Long> categoriaIds, YearMonth mes) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            return resultado;
        }
        for (Object[] linha : contaRepository.sumGastosPorCategoriasBatch(
                user, new ArrayList<>(categoriaIds), mes.atDay(1), mes.plusMonths(1).atDay(1))) {
            resultado.put((Long) linha[0], paraBigDecimal(linha[1]));
        }
        return resultado;
    }

    public boolean possuiContas(User user, EscopoTransacao escopo) {
        return contaRepository.existsByResponsavelAndEscopo(user, escopo);
    }

    private static ResumoAtraso paraAtraso(ResumoPendente r) {
        BigDecimal total = r.total() != null ? r.total() : BigDecimal.ZERO;
        return new ResumoAtraso(r.quantidade() != null ? r.quantidade() : 0L, total);
    }

    private static BigDecimal paraBigDecimal(Object valor) {
        return valor instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(valor));
    }
}
