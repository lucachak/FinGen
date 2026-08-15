package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/app/dashboard")
@Slf4j
public class DashboardController {

    private final ContaRepository contaRepository;
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

    public DashboardController(ContaRepository contaRepository, UsuarioRepository usuarioRepository,
            OrcamentoRepository orcamentoRepository,
            WealthSnapshotCacheService wealthSnapshotCacheService,
            AiHealthCacheService aiHealthCacheService,
            ObjectMapper objectMapper, ContaService contaService,
            MetaFinanceiraRepository metaRepository,
            FinancialHealthService financialHealthService,
            PortfolioInsightService portfolioInsightService,
            ConfiguracaoFinanceiraRepository configuracaoFinanceiraRepository,
            EspacoFinanceiroService espacoFinanceiroService) {
        this.contaRepository = contaRepository;
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

        // --- OTIMIZAÇÃO: 1 query unificada por escopo ---
        // Todos os escopos em uma única query, filtrado em memória
        List<Conta> todasContas = contaRepository.findAllByResponsavelOrderByDataVencimentoAsc(usuarioLogado);
        YearMonth mesAtual = YearMonth.now();

        List<Conta> contasCasa = todasContas.stream().filter(c -> c.getEscopo() == EscopoTransacao.CASA).toList();
        List<Conta> contasPessoais = todasContas.stream().filter(c -> c.getEscopo() == EscopoTransacao.PESSOAL)
                .toList();
        List<Conta> contasNegocio = todasContas.stream().filter(c -> c.getEscopo() == EscopoTransacao.NEGOCIO).toList();

        // 1. Cálculos Base (Totais Mensais)
        model.addAttribute("gastoCasaMes", calcularTotal(contasCasa, TipoTransacao.DESPESA, mesAtual));
        model.addAttribute("pendenteCasaMes", calcularPendente(contasCasa, TipoTransacao.DESPESA, mesAtual));

        BigDecimal despesaPessoal = calcularTotal(contasPessoais, TipoTransacao.DESPESA, mesAtual);
        BigDecimal pendentePessoal = calcularPendente(contasPessoais, TipoTransacao.DESPESA, mesAtual);
        BigDecimal receitaPessoalBruta = calcularTotal(contasPessoais, TipoTransacao.RECEITA, mesAtual);

        // Ativos de renda são buscados via filtro na lista já carregada (INCOME assets
        // tratado via WealthService)
        // Mantemos a chamada isolada pois é leve (só assets de tipo INCOME)
        BigDecimal receitaPessoal = receitaPessoalBruta; // wealth income assets adicionados no FinancialHealthService

        model.addAttribute("gastoPessoalMes", despesaPessoal);
        model.addAttribute("pendentePessoalMes", pendentePessoal);
        model.addAttribute("receitaPessoalMes", receitaPessoal);
        model.addAttribute("gastoNegocioMes", calcularTotal(contasNegocio, TipoTransacao.DESPESA, mesAtual));
        model.addAttribute("pendenteNegocioMes", calcularPendente(contasNegocio, TipoTransacao.DESPESA, mesAtual));

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
        Map<String, BigDecimal> gastosPorCategoriaCasa = contasCasa.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(mesAtual))
                .collect(Collectors.groupingBy(
                        c -> c.getCategoria() != null ? c.getCategoria().getNome() : "Outros",
                        Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)));
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

        Map<Long, BigDecimal> gastosBatch = new HashMap<>();
        if (!catIds.isEmpty()) {
            int mes = java.time.LocalDate.now().getMonthValue();
            int ano = java.time.LocalDate.now().getYear();
            List<Object[]> rows = contaRepository.sumGastosPorCategoriasBatch(usuarioLogado, catIds, mes, ano);
            for (Object[] row : rows) {
                Long catId = (Long) row[0];
                BigDecimal soma = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                gastosBatch.put(catId, soma);
            }
        }

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
        FinancialHealthService.HealthReport healthReport = financialHealthService.calcular(usuarioLogado, contasPessoais,
                orcamentos);
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
        List<Conta> recentTransactions = contasPessoais.stream()
                .filter(Conta::isPaga)
                .filter(c -> c.getDataPagamento() != null)
                .sorted(java.util.Comparator.comparing(Conta::getDataPagamento, java.util.Comparator.reverseOrder()))
                .limit(5)
                .toList();
        model.addAttribute("recentTransactions", recentTransactions);

        LocalDate hoje = LocalDate.now();
        List<Conta> contasPendentes = contasPessoais.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA && !c.isPaga())
                .filter(c -> c.getDataVencimento() != null)
                .toList();
        List<Conta> contasAtrasadas = contasPendentes.stream()
                .filter(c -> c.getDataVencimento().isBefore(hoje))
                .sorted(java.util.Comparator.comparing(Conta::getDataVencimento))
                .toList();
        List<Conta> vencendoEmSeteDias = contasPendentes.stream()
                .filter(c -> !c.getDataVencimento().isBefore(hoje))
                .filter(c -> !c.getDataVencimento().isAfter(hoje.plusDays(7)))
                .sorted(java.util.Comparator.comparing(Conta::getDataVencimento))
                .toList();
        List<Conta> proximosVencimentos = contasPendentes.stream()
                .filter(c -> !c.getDataVencimento().isBefore(hoje))
                .sorted(java.util.Comparator.comparing(Conta::getDataVencimento))
                .limit(5)
                .toList();

        model.addAttribute("upcomingBills", proximosVencimentos);
        model.addAttribute("quantidadeAtrasadas", contasAtrasadas.size());
        model.addAttribute("totalAtrasado", somarValores(contasAtrasadas));
        model.addAttribute("quantidadeVencendo7Dias", vencendoEmSeteDias.size());
        model.addAttribute("totalVencendo7Dias", somarValores(vencendoEmSeteDias));
        model.addAttribute("temDadosFinanceiros", !contasPessoais.isEmpty());

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

    private BigDecimal calcularTotal(List<Conta> contas, TipoTransacao tipo, YearMonth mes) {
        return contas.stream()
                .filter(c -> c.getTipo() == tipo)
                .filter(Conta::isPaga)
                .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(mes))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPendente(List<Conta> contas, TipoTransacao tipo, YearMonth mes) {
        return contas.stream()
                .filter(c -> c.getTipo() == tipo)
                .filter(c -> !c.isPaga())
                .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(mes))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarValores(List<Conta> contas) {
        return contas.stream()
                .map(Conta::getValor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
