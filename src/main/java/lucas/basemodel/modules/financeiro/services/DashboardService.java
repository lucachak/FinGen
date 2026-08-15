package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.DashboardSummaryResponse;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public DashboardSummaryResponse getSummary(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);
        List<Conta> todas = contaRepository.findAllByResponsavelOrderByDataVencimentoAsc(user);
        List<Conta> selecionadas = porEscopo(todas, escopo);
        YearMonth mes = YearMonth.now();

        BigDecimal despesasPendentes = total(selecionadas, TipoTransacao.DESPESA, false, mes);
        BigDecimal receitas = total(selecionadas, TipoTransacao.RECEITA, true, mes);
        BigDecimal despesasPagas = total(selecionadas, TipoTransacao.DESPESA, true, mes);

        return DashboardSummaryResponse.builder()
                .escopo(escopo)
                .gastosCasa(total(porEscopo(todas, EscopoTransacao.CASA), TipoTransacao.DESPESA, true, mes))
                .gastosPessoal(total(porEscopo(todas, EscopoTransacao.PESSOAL), TipoTransacao.DESPESA, true, mes))
                .gastosNegocio(total(porEscopo(todas, EscopoTransacao.NEGOCIO), TipoTransacao.DESPESA, true, mes))
                .totalReceitas(receitas)
                .totalDespesas(despesasPagas.add(despesasPendentes))
                .totalDespesasPendentes(despesasPendentes)
                .freeCashFlow(receitas.subtract(despesasPagas).subtract(despesasPendentes))
                .build();
    }

    public Map<String, Object> getChartData(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);
        return contaService.obterDadosDonutMesAtual(user, escopo);
    }

    public List<ContaResponse> getProximasContas(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);
        return contaService.listarContasAVencer(user).stream()
                .filter(c -> c.getEscopo() == escopo)
                .limit(5)
                .map(contaService::toResponse)
                .toList();
    }

    public List<ContaResponse> getTransacoesRecentes(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);
        return contaRepository.findByResponsavelAndPagaTrueOrderByDataPagamentoDesc(user).stream()
                .filter(c -> c.getEscopo() == escopo)
                .limit(5)
                .map(contaService::toResponse)
                .toList();
    }

    private List<Conta> porEscopo(List<Conta> contas, EscopoTransacao escopo) {
        return contas.stream().filter(c -> c.getEscopo() == escopo).toList();
    }

    private BigDecimal total(List<Conta> contas, TipoTransacao tipo, boolean paga, YearMonth mes) {
        return contas.stream()
                .filter(c -> c.getTipo() == tipo && c.isPaga() == paga)
                .filter(c -> {
                    LocalDate data = paga ? c.getDataPagamento() : c.getDataVencimento();
                    return data != null && YearMonth.from(data).equals(mes);
                })
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
