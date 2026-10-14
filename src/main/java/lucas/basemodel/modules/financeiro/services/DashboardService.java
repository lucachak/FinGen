package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.financeiro.dto.DashboardSummaryResponse;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.TotaisMes;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LIMITE_LISTAS = 5;

    private final DashboardQueryService consulta;
    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public DashboardSummaryResponse getSummary(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);

        Map<EscopoTransacao, TotaisMes> totais = consulta.totaisDoMes(user, YearMonth.now());
        TotaisMes selecionado = totais.get(escopo);

        BigDecimal despesasPendentes = selecionado.despesaPendente();
        BigDecimal receitas = selecionado.receitaPaga();
        BigDecimal despesasPagas = selecionado.despesaPaga();

        return DashboardSummaryResponse.builder()
                .escopo(escopo)
                .gastosCasa(totais.get(EscopoTransacao.CASA).despesaPaga())
                .gastosPessoal(totais.get(EscopoTransacao.PESSOAL).despesaPaga())
                .gastosNegocio(totais.get(EscopoTransacao.NEGOCIO).despesaPaga())
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
        return consulta.proximasPendentes(user, escopo, null, LocalDate.now(), LIMITE_LISTAS).stream()
                .map(contaService::toResponse)
                .toList();
    }

    public List<ContaResponse> getTransacoesRecentes(String email, EscopoTransacao escopo) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, escopo);
        return consulta.pagasRecentes(user, escopo, LIMITE_LISTAS).stream()
                .map(contaService::toResponse)
                .toList();
    }
}
