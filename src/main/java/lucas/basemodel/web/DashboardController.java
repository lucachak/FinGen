package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.ResumoAtraso;
import lucas.basemodel.modules.financeiro.services.DashboardQueryService.TotaisMes;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.services.WealthSnapshotCacheService;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.services.AiHealthCacheService;
import lucas.basemodel.modules.financeiro.services.ContaService;
import lucas.basemodel.modules.financeiro.services.FinancialHealthService;
import lucas.basemodel.modules.financeiro.services.PortfolioInsightService;
import lucas.basemodel.modules.financeiro.services.EspacoFinanceiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/app/dashboard")
@Slf4j
public class DashboardController {

    private final DashboardQueryService dashboardQuery;
    private final UsuarioRepository usuarioRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final WealthSnapshotCacheService wealthSnapshotCacheService;
    private final AiHealthCacheService aiHealthCacheService;
    private final ObjectMapper objectMapper;
    private final ContaService contaService;
    private final MetaFinanceiraRepository metaRepository;
    private final FinancialHealthService financialHealthService;
    private final PortfolioInsightService portfolioInsightService;
    private final ConfiguracaoFinanceiraRepository configuracaoFinanceiraRepository;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public DashboardController(DashboardQueryService dashboardQuery, UsuarioRepository usuarioRepository,
            OrcamentoRepository orcamentoRepository,
            WealthSnapshotCacheService wealthSnapshotCacheService,
            AiHealthCacheService aiHealthCacheService,
            ObjectMapper objectMapper, ContaService contaService,
            MetaFinanceiraRepository metaRepository,
            FinancialHealthService financialHealthService,
            PortfolioInsightService portfolioInsightService,
            ConfiguracaoFinanceiraRepository configuracaoFinanceiraRepository,
            EspacoFinanceiroService espacoFinanceiroService) {
        this.dashboardQuery = dashboardQuery;
        this.usuarioRepository = usuarioRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.wealthSnapshotCacheService = wealthSnapshotCacheService;
        this.aiHealthCacheService = aiHealthCacheService;
        this.objectMapper = objectMapper;
        this.contaService = contaService;
        this.metaRepository = metaRepository;
        this.financialHealthService = financialHealthService;
        this.portfolioInsightService = portfolioInsightService;
        this.configuracaoFinanceiraRepository = configuracaoFinanceiraRepository;
        this.espacoFinanceiroService = espacoFinanceiroService;
    }

    @GetMapping
    public String index(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/auth/login";

        User usuarioLogado = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

        if (!usuarioLogado.isSetupCompleted()) {
            return "redirect:/app/onboarding";
        }

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("user", usuarioLogado);

        // --- CONFIGURAÇÃO FINANCEIRA (escopo / perfil do usuário) ---
        Optional<ConfiguracaoFinanceira> configOpt = configuracaoFinanceiraRepository.findByUser(usuarioLogado);
        ConfiguracaoFinanceira configuracaoFinanceira = configOpt.orElse(null);
        model.addAttribute("configuracaoFinanceira", configuracaoFinanceira);

        // Flags para renderização condicional das tabs
        boolean moraCom = configuracaoFinanceira != null
                && configuracaoFinanceira.getSituacaoMoradia() == SituacaoMoradia.COM_OS_PAIS;
        boolean possuiNegocio = configuracaoFinanceira != null && configuracaoFinanceira.isPossuiNegocio();
        model.addAttribute("mostrarTabCasa", !moraCom);
        model.addAttribute("mostrarTabNegocio", possuiNegocio);

        // --- Totais do mês agregados no banco (1 query, em vez de carregar todas as contas) ---
        YearMonth mesAtual = YearMonth.now();
        LocalDate hoje = LocalDate.now();
        Map<EscopoTransacao, TotaisMes> totaisDoMes = dashboardQuery.totaisDoMes(usuarioLogado, mesAtual);
        TotaisMes totaisCasa = totaisDoMes.get(EscopoTransacao.CASA);
        TotaisMes totaisPessoal = totaisDoMes.get(EscopoTransacao.PESSOAL);
        TotaisMes totaisNegocio = totaisDoMes.get(EscopoTransacao.NEGOCIO);

        // 1. Cálculos Base (Totais Mensais)
        model.addAttribute("gastoCasaMes", totaisCasa.despesaPaga());
        model.addAttribute("pendenteCasaMes", totaisCasa.despesaPendente());

        BigDecimal despesaPessoal = totaisPessoal.despesaPaga();
        BigDecimal pendentePessoal = totaisPessoal.despesaPendente();
        // wealth income assets são somados depois, no FinancialHealthService
        BigDecimal receitaPessoal = totaisPessoal.receitaPaga();

        model.addAttribute("gastoPessoalMes", despesaPessoal);
        model.addAttribute("pendentePessoalMes", pendentePessoal);
        model.addAttribute("receitaPessoalMes", receitaPessoal);
        model.addAttribute("gastoNegocioMes", totaisNegocio.despesaPaga());
        model.addAttribute("pendenteNegocioMes", totaisNegocio.despesaPendente());

        // Cálculo de percentual de gasto pessoal
        String percentualGastoTexto = "Sob controle";
        if (despesaPessoal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = receitaPessoal.compareTo(BigDecimal.ZERO) > 0 ? receitaPessoal : BigDecimal.ONE;
            BigDecimal perc = despesaPessoal.multiply(new BigDecimal("100"))
                    .divide(divisor, 0, java.math.RoundingMode.HALF_UP);
            percentualGastoTexto = perc + "% da receita";
        }
        model.addAttribute("percentualGastoPessoal", percentualGastoTexto);

        // 2. Gráfico Doughnut da Casa
        Map<String, BigDecimal> gastosPorCategoriaCasa = dashboardQuery.despesasPorCategoria(
                usuarioLogado, EscopoTransacao.CASA, mesAtual);
        model.addAttribute("labelsCategoriaCasa", gastosPorCategoriaCasa.keySet());
        model.addAttribute("valoresCategoriaCasa", gastosPorCategoriaCasa.values());

        // Orçamento Mensal
        BigDecimal orcamentoMensal = (usuarioLogado.getOrcamentoMensal() != null)
                ? usuarioLogado.getOrcamentoMensal()
                : new BigDecimal("3500.00");
        model.addAttribute("orcamentoMensal", orcamentoMensal);

        // --- WEALTH MANAGEMENT (via cache — não bloqueia o request) ---
        WealthSnapshot latest = wealthSnapshotCacheService.getSnapshot(usuarioLogado);
        model.addAttribute("patrimonioTotal", latest != null ? latest.getTotalNetWorth() : BigDecimal.ZERO);

        // Historia via snapshot repository direto — evita recalcular
        List<lucas.basemodel.modules.wealth.models.WealthSnapshot> historyFull = wealthSnapshotCacheService
                .getSnapshotHistory(usuarioLogado.getId(), 12);
        java.util.Collections.reverse(historyFull);
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("MMM/yy");
        java.util.List<String> mesesHistorico = new java.util.ArrayList<>();
        java.util.List<BigDecimal> valoresHistorico = new java.util.ArrayList<>();
        for (lucas.basemodel.modules.wealth.models.WealthSnapshot ws : historyFull) {
            mesesHistorico.add(ws.getCreatedAt().format(formatador).toUpperCase());
            valoresHistorico.add(ws.getTotalNetWorth());
        }
        model.addAttribute("labelsHistoricoPatrimonio", mesesHistorico);
        model.addAttribute("valoresHistoricoPatrimonio", valoresHistorico);
        model.addAttribute("snapshotAtualizadoEm", latest != null ? latest.getCreatedAt() : null);
        BigDecimal variacaoPatrimonio = null;
        if (historyFull.size() >= 2) {
            BigDecimal anterior = historyFull.get(historyFull.size() - 2).getTotalNetWorth();
            BigDecimal atual = historyFull.get(historyFull.size() - 1).getTotalNetWorth();
            if (anterior != null && anterior.compareTo(BigDecimal.ZERO) != 0 && atual != null) {
                variacaoPatrimonio = atual.subtract(anterior)
                        .multiply(new BigDecimal("100"))
                        .divide(anterior.abs(), 1, java.math.RoundingMode.HALF_UP);
            }
        }
        model.addAttribute("variacaoPatrimonio", variacaoPatrimonio);
        model.addAttribute("suggestions", wealthSnapshotCacheService.getSuggestions(usuarioLogado.getId()));

        if (latest != null && latest.getBreakdownJson() != null) {
            try {
                Map<String, BigDecimal> breakdown = objectMapper.readValue(latest.getBreakdownJson(), Map.class);
                model.addAttribute("assetBreakdownLabels", breakdown.keySet());
                model.addAttribute("assetBreakdownValues", breakdown.values());
            } catch (Exception e) {
                log.error("Error parsing breakdown JSON", e);
            }
        }

        // --- ALERTA ORÇAMENTOS (batch query — anti N+1) ---
        List<Orcamento> orcamentos = orcamentoRepository.findByResponsavel(usuarioLogado).stream()
                .filter(o -> o.getCategoria() != null && o.getCategoria().getEscopo() == EscopoTransacao.PESSOAL)
                .toList();
        List<Long> catIds = orcamentos.stream()
                .filter(o -> o.getCategoria() != null)
                .map(o -> o.getCategoria().getId())
                .toList();

        Map<Long, BigDecimal> gastosBatch = dashboardQuery.gastosPagosPorCategoria(usuarioLogado, catIds, mesAtual);

        List<Map<String, Object>> orcamentosAlerta = new java.util.ArrayList<>();
        for (Orcamento orc : orcamentos) {
            Long catId = orc.getCategoria() != null ? orc.getCategoria().getId() : null;
            BigDecimal gasto = catId != null ? gastosBatch.getOrDefault(catId, BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal limite = orc.getLimiteMensal();
            if (limite != null && limite.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentual = gasto.multiply(new BigDecimal("100")).divide(limite, 2,
                        java.math.RoundingMode.HALF_UP);
                if (percentual.compareTo(new BigDecimal("80")) >= 0) {
                    Map<String, Object> alerta = new java.util.HashMap<>();
                    alerta.put("categoria", orc.getCategoria());
                    alerta.put("gasto", gasto);
                    alerta.put("limite", limite);
                    alerta.put("percentual", percentual);
                    orcamentosAlerta.add(alerta);
                }
            }
        }
        model.addAttribute("orcamentosAlerta", orcamentosAlerta);

        // --- FREE CASH FLOW ---
        // Os indicadores exibidos dentro da aba Pessoal usam apenas esse espaço.
        BigDecimal receitaTotalMes = receitaPessoal;
        model.addAttribute("receitaTotalMes", receitaTotalMes);

        BigDecimal totalPassivos = despesaPessoal.add(pendentePessoal);

        BigDecimal freeCashFlow = receitaTotalMes.subtract(totalPassivos);
        model.addAttribute("totalAtivos", receitaTotalMes);
        model.addAttribute("totalPassivos", totalPassivos);
        model.addAttribute("freeCashFlow", freeCashFlow);

        PortfolioInsightService.PortfolioOverview portfolio = portfolioInsightService.build(
                usuarioLogado, freeCashFlow.max(BigDecimal.ZERO));
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("portfolioAllocationLabels", portfolio.allocations().stream()
                .filter(allocation -> allocation.value().compareTo(BigDecimal.ZERO) > 0)
                .map(PortfolioInsightService.Allocation::label)
                .toList());
        model.addAttribute("portfolioAllocationValues", portfolio.allocations().stream()
                .filter(allocation -> allocation.value().compareTo(BigDecimal.ZERO) > 0)
                .map(PortfolioInsightService.Allocation::value)
                .toList());
        model.addAttribute("portfolioAllocationColors", portfolio.allocations().stream()
                .filter(allocation -> allocation.value().compareTo(BigDecimal.ZERO) > 0)
                .map(PortfolioInsightService.Allocation::color)
                .toList());

        // --- MODELO FINANCEIRO RIGOROSO (FinancialHealthService) ---
        FinancialHealthService.HealthReport healthReport = financialHealthService.calcular(usuarioLogado, totaisPessoal,
                dashboardQuery.atrasadas(usuarioLogado, EscopoTransacao.PESSOAL, hoje), orcamentos);
        model.addAttribute("scoreSaude", healthReport.scoreSaude());
        model.addAttribute("taxaPoupancaEfetiva", healthReport.taxaPoupancaEfetiva());
        model.addAttribute("comprometimentoRenda", healthReport.comprometimentoRenda());
        model.addAttribute("classificacaoSaude", healthReport.classificacao());
        model.addAttribute("projecao3Meses", healthReport.projecao3Meses());
        model.addAttribute("comprometimentoVisual", healthReport.comprometimentoRenda()
                .max(BigDecimal.ZERO).min(new BigDecimal("100")));
        model.addAttribute("poupancaVisual", healthReport.taxaPoupancaEfetiva()
                .max(BigDecimal.ZERO).min(new BigDecimal("100")));

        // --- GESTOR AI DATA (lazy via HTMX — não bloqueia o render inicial) ---
        // aiOnline usa cache — nunca faz HTTP no request thread
        model.addAttribute("aiOnline", aiHealthCacheService.isOnline());

        // --- OTHER DATA ---
        model.addAttribute("metas", metaRepository.findByResponsavel(usuarioLogado));
        List<Conta> recentTransactions = dashboardQuery.pagasRecentes(usuarioLogado, EscopoTransacao.PESSOAL, 5);
        model.addAttribute("recentTransactions", recentTransactions);

        ResumoAtraso atrasadas = dashboardQuery.atrasadas(usuarioLogado, EscopoTransacao.PESSOAL, hoje);
        ResumoAtraso vencendoEmSeteDias = dashboardQuery.vencendo(usuarioLogado, EscopoTransacao.PESSOAL, hoje, 7);
        List<Conta> proximosVencimentos = dashboardQuery.proximasPendentes(
                usuarioLogado, EscopoTransacao.PESSOAL, TipoTransacao.DESPESA, hoje, 5);

        model.addAttribute("upcomingBills", proximosVencimentos);
        model.addAttribute("quantidadeAtrasadas", atrasadas.quantidade());
        model.addAttribute("totalAtrasado", atrasadas.total());
        model.addAttribute("quantidadeVencendo7Dias", vencendoEmSeteDias.quantidade());
        model.addAttribute("totalVencendo7Dias", vencendoEmSeteDias.total());
        model.addAttribute("temDadosFinanceiros", dashboardQuery.possuiContas(usuarioLogado, EscopoTransacao.PESSOAL));
        // Posição de "hoje" na faixa do mês (dashboard)
        model.addAttribute("diaDoMes", hoje.getDayOfMonth());
        model.addAttribute("diasNoMes", mesAtual.lengthOfMonth());

        return "dashboard/index";
    }

    /**
     * Endpoint lazy para dados dos gráficos de IA — chamado via HTMX após o paint
     * inicial
     */
    @GetMapping(value = "/chart-data", produces = "application/json")
    @ResponseBody
    public Map<String, Object> chartData(@RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo,
                                         Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        espacoFinanceiroService.validarAcesso(usuarioLogado, escopo);
        Map<String, Object> data = new HashMap<>();
        data.put("escopo", escopo);
        data.put("dadosCategoria", contaService.obterGastosPorCategoriaMesAtual(usuarioLogado, escopo));
        data.put("dadosFluxo", contaService.obterFluxoCaixaUltimos6Meses(usuarioLogado, escopo));
        return data;
    }
}
