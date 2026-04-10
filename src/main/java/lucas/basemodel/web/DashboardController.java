package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.financeiro.repositories.HistoricoPatrimonioRepository;
import lucas.basemodel.modules.financeiro.services.HistoricoPatrimonioService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.repositories.WealthSuggestionRepository;
import lucas.basemodel.modules.wealth.services.WealthService;
import lucas.basemodel.modules.wealth.services.WealthSuggestionService;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.services.ContaService;
import lucas.basemodel.modules.financeiro.services.GeminiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final WealthService wealthService;
    private final WealthSuggestionRepository suggestionRepository;
    private final WealthSuggestionService suggestionService;
    private final ObjectMapper objectMapper;
    private final ContaService contaService;
    private final GeminiService geminiService;
    private final MetaFinanceiraRepository metaRepository;

    public DashboardController(ContaRepository contaRepository, UsuarioRepository usuarioRepository, 
                               OrcamentoRepository orcamentoRepository, WealthService wealthService, 
                               WealthSuggestionRepository suggestionRepository, WealthSuggestionService suggestionService, 
                               ObjectMapper objectMapper, ContaService contaService, 
                               GeminiService geminiService, MetaFinanceiraRepository metaRepository) {
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.wealthService = wealthService;
        this.suggestionRepository = suggestionRepository;
        this.suggestionService = suggestionService;
        this.objectMapper = objectMapper;
        this.contaService = contaService;
        this.geminiService = geminiService;
        this.metaRepository = metaRepository;
    }

    @GetMapping
    public String index(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        
        if (!usuarioLogado.isSetupCompleted()) {
            return "redirect:/app/onboarding";
        }

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("user", usuarioLogado);

        // --- DASHBOARD OVERVIEW LOGIC ---
        
        // Separação de Escopos
        List<Conta> contasCasa = contaRepository.findByResponsavelAndEscopo(usuarioLogado, EscopoTransacao.CASA);
        List<Conta> contasPessoais = contaRepository.findByResponsavelAndEscopo(usuarioLogado, EscopoTransacao.PESSOAL);
        List<Conta> contasNegocio = contaRepository.findByResponsavelAndEscopo(usuarioLogado, EscopoTransacao.NEGOCIO);

        // 1. Cálculos Base (Totais Mensais)
        model.addAttribute("gastoCasaMes", calcularTotal(contasCasa, TipoTransacao.DESPESA));
        model.addAttribute("pendenteCasaMes", calcularPendente(contasCasa, TipoTransacao.DESPESA));

        BigDecimal despesaPessoal = calcularTotal(contasPessoais, TipoTransacao.DESPESA);
        BigDecimal pendentePessoal = calcularPendente(contasPessoais, TipoTransacao.DESPESA);
        BigDecimal receitaPessoalList = calcularTotal(contasPessoais, TipoTransacao.RECEITA);
        
        BigDecimal monthlyIncomeAssets = wealthService.getMonthlyIncomeAssets(usuarioLogado);
        BigDecimal receitaPessoal = receitaPessoalList.add(monthlyIncomeAssets);
        
        model.addAttribute("gastoPessoalMes", despesaPessoal);
        model.addAttribute("pendentePessoalMes", pendentePessoal);
        model.addAttribute("receitaPessoalMes", receitaPessoal);
        model.addAttribute("gastoNegocioMes", calcularTotal(contasNegocio, TipoTransacao.DESPESA));
        model.addAttribute("pendenteNegocioMes", calcularPendente(contasNegocio, TipoTransacao.DESPESA));

        // Cálculo de percentual de gasto pessoal
        String percentualGastoTexto = "Sob controle";
        if (despesaPessoal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = receitaPessoal.compareTo(BigDecimal.ZERO) > 0 ? receitaPessoal : BigDecimal.ONE;
            BigDecimal perc = despesaPessoal.multiply(new BigDecimal("100")).divide(divisor, 0, java.math.RoundingMode.HALF_UP);
            percentualGastoTexto = perc.toString() + "% da receita";
        }
        model.addAttribute("percentualGastoPessoal", percentualGastoTexto);

        // 2. Gráfico Doughnut da Casa
        YearMonth mesAtual = YearMonth.now();
        Map<String, BigDecimal> gastosPorCategoriaCasa = contasCasa.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(mesAtual))
                .collect(Collectors.groupingBy(
                        c -> c.getCategoria() != null ? c.getCategoria().getNome() : "Outros",
                        Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)
                ));

        model.addAttribute("labelsCategoriaCasa", gastosPorCategoriaCasa.keySet());
        model.addAttribute("valoresCategoriaCasa", gastosPorCategoriaCasa.values());

        // Orçamento Mensal
        BigDecimal orcamentoMensal = (usuarioLogado.getOrcamentoMensal() != null)
                ? usuarioLogado.getOrcamentoMensal() : new BigDecimal("3500.00");
        model.addAttribute("orcamentoMensal", orcamentoMensal);

        // --- WEALTH MANAGEMENT ---
        wealthService.updateAllValuations(usuarioLogado.getId());
        WealthSnapshot latest = wealthService.createSnapshot(usuarioLogado);
        suggestionService.generateSuggestions(usuarioLogado, latest);
        
        model.addAttribute("patrimonioTotal", latest.getTotalNetWorth());
        
        List<WealthSnapshot> history = wealthService.getHistory(usuarioLogado.getId());
        if (history.size() > 12) history = history.subList(0, 12);
        java.util.Collections.reverse(history);
        
        List<String> mesesHistorico = new java.util.ArrayList<>();
        List<BigDecimal> valoresHistorico = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("MMM/yy");
        for (WealthSnapshot ws : history) {
            mesesHistorico.add(ws.getCreatedAt().format(formatador).toUpperCase());
            valoresHistorico.add(ws.getTotalNetWorth());
        }
        model.addAttribute("labelsHistoricoPatrimonio", mesesHistorico);
        model.addAttribute("valoresHistoricoPatrimonio", valoresHistorico);
        model.addAttribute("suggestions", suggestionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(usuarioLogado.getId()));

        if (latest.getBreakdownJson() != null) {
            try {
                Map<String, BigDecimal> breakdown = objectMapper.readValue(latest.getBreakdownJson(), Map.class);
                model.addAttribute("assetBreakdownLabels", breakdown.keySet());
                model.addAttribute("assetBreakdownValues", breakdown.values());
            } catch (Exception e) {
                log.error("Error parsing breakdown JSON", e);
            }
        }

        // --- ALERTA ORÇAMENTOS ---
        List<Orcamento> orcamentos = orcamentoRepository.findByResponsavel(usuarioLogado);
        List<Map<String, Object>> orcamentosAlerta = new java.util.ArrayList<>();
        int mesAtualNum = java.time.LocalDate.now().getMonthValue();
        int anoAtualNum = java.time.LocalDate.now().getYear();

        for (Orcamento orc : orcamentos) {
            BigDecimal gasto = contaRepository.sumGastosPorCategoriaMesAno(usuarioLogado, orc.getCategoria(), mesAtualNum, anoAtualNum);
            if (gasto == null) gasto = BigDecimal.ZERO;
            BigDecimal limite = orc.getLimiteMensal();
            if (limite != null && limite.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentual = gasto.multiply(new BigDecimal("100")).divide(limite, 2, java.math.RoundingMode.HALF_UP);
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
        BigDecimal receitaCasa = calcularTotal(contasCasa, TipoTransacao.RECEITA);
        BigDecimal receitaNegocio = calcularTotal(contasNegocio, TipoTransacao.RECEITA);

        // Receita Total do Mês: soma TODOS os escopos (Pessoal + Casa + Negócio + Ativos)
        // Este é o valor correto para o card "Ganhos do Mês" no dashboard global.
        BigDecimal receitaTotalMes = receitaPessoal.add(receitaCasa).add(receitaNegocio);
        model.addAttribute("receitaTotalMes", receitaTotalMes);

        BigDecimal totalReceitas = receitaTotalMes; // alias para o Free Cash Flow abaixo
        
        // Total de Passivos do mês (Realizado + Pendente) para um Free Cash Flow precavido
        BigDecimal totalPassivos = despesaPessoal.add(pendentePessoal)
                .add(calcularTotal(contasCasa, TipoTransacao.DESPESA)).add(calcularPendente(contasCasa, TipoTransacao.DESPESA))
                .add(calcularTotal(contasNegocio, TipoTransacao.DESPESA)).add(calcularPendente(contasNegocio, TipoTransacao.DESPESA));
        
        BigDecimal freeCashFlow = totalReceitas.subtract(totalPassivos);
        
        model.addAttribute("totalAtivos", totalReceitas);
        model.addAttribute("totalPassivos", totalPassivos);
        model.addAttribute("freeCashFlow", freeCashFlow);

        // --- GESTOR AI DATA ---
        model.addAttribute("dadosCategoria", contaService.obterGastosPorCategoriaMesAtual(usuarioLogado));
        model.addAttribute("dadosFluxo", contaService.obterFluxoCaixaUltimos6Meses(usuarioLogado));
        model.addAttribute("aiOnline", geminiService.isServiceAvailable());

        // --- NEW DASHBOARD DATA ---
        model.addAttribute("metas", metaRepository.findByResponsavel(usuarioLogado));
        model.addAttribute("recentTransactions", contaRepository.findTop5ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(usuarioLogado));
        model.addAttribute("upcomingBills", contaRepository.findByResponsavelAndPagaFalseOrderByDataVencimentoAsc(usuarioLogado));

        return "dashboard/index";
    }

    @GetMapping(value = "/chart-data", produces = "application/json")
    @ResponseBody
    public Map<String, Object> chartData(Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        Map<String, Object> data = new HashMap<>();
        data.put("dadosCategoria", contaService.obterGastosPorCategoriaMesAtual(usuarioLogado));
        data.put("dadosFluxo", contaService.obterFluxoCaixaUltimos6Meses(usuarioLogado));
        return data;
    }

    private BigDecimal calcularTotal(List<Conta> contas, TipoTransacao tipo) {
        YearMonth mesAtual = YearMonth.now();
        return contas.stream()
                .filter(c -> c.getTipo() == tipo)
                .filter(Conta::isPaga)
                .filter(c -> c.getDataPagamento() != null && YearMonth.from(c.getDataPagamento()).equals(mesAtual))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPendente(List<Conta> contas, TipoTransacao tipo) {
        YearMonth mesAtual = YearMonth.now();
        return contas.stream()
                .filter(c -> c.getTipo() == tipo)
                .filter(c -> !c.isPaga())
                .filter(c -> c.getDataVencimento() != null && YearMonth.from(c.getDataVencimento()).equals(mesAtual))
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
