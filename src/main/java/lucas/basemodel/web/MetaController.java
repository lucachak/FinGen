package lucas.basemodel.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.services.OpenRouterService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.web.dto.MetaSugestaoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/app/financeiro/metas")
@lombok.extern.slf4j.Slf4j
public class MetaController {

    private final MetaFinanceiraRepository metaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OpenRouterService openRouterService;
    private final InvestimentoRepository investimentoRepository;
    private final ObjectMapper objectMapper;

    public MetaController(MetaFinanceiraRepository metaRepository,
                          UsuarioRepository usuarioRepository,
                          OpenRouterService openRouterService,
                          InvestimentoRepository investimentoRepository) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
        this.openRouterService = openRouterService;
        this.investimentoRepository = investimentoRepository;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping({"", "/"})
    public String listar(Model model, Principal principal) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            List<MetaFinanceira> metas = metaRepository.findByResponsavel(user);
            
            Map<UUID, BigDecimal> percentuais = new HashMap<>();
            Map<UUID, BigDecimal> aportesNecessarios = new HashMap<>();
            Map<UUID, Long> mesesRestantes = new HashMap<>();

            for (MetaFinanceira meta : metas) {
                BigDecimal percentual = BigDecimal.ZERO;
                if (meta.getValorAlvo().compareTo(BigDecimal.ZERO) > 0) {
                    percentual = meta.getValorAtual().multiply(new BigDecimal("100"))
                                     .divide(meta.getValorAlvo(), 2, RoundingMode.HALF_UP);
                }
                percentuais.put(meta.getId(), percentual);

                long meses = 0;
                if (meta.getPrazo() != null) {
                    meses = ChronoUnit.MONTHS.between(LocalDate.now(), meta.getPrazo());
                    if (meses < 0) meses = 0;
                }
                mesesRestantes.put(meta.getId(), meses);

                BigDecimal restante = meta.getValorAlvo().subtract(meta.getValorAtual());
                BigDecimal aporte = BigDecimal.ZERO;
                if (restante.compareTo(BigDecimal.ZERO) > 0) {
                    long div = meses > 0 ? meses : 1;
                    aporte = restante.divide(new BigDecimal(div), 2, RoundingMode.HALF_UP);
                }
                aportesNecessarios.put(meta.getId(), aporte);
            }

            BigDecimal totalAlmejado = metas.stream()
                .map(m -> m.getValorAlvo() != null ? m.getValorAlvo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPoupado = metas.stream()
                .map(m -> m.getValorAtual() != null ? m.getValorAtual() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("metas", metas);
            model.addAttribute("totalAlmejado", totalAlmejado);
            model.addAttribute("totalPoupado", totalPoupado);
            model.addAttribute("percentuais", percentuais);
            model.addAttribute("aportesNecessarios", aportesNecessarios);
            model.addAttribute("mesesRestantes", mesesRestantes);
        }
        model.addAttribute("activeMenu", "metas");
        return "metas/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("meta", new MetaFinanceira());
        model.addAttribute("naturezas", NaturezaMeta.values());
        model.addAttribute("activeMenu", "metas");
        return "metas/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable UUID id, Model model) {
        MetaFinanceira meta = metaRepository.findById(id).orElse(null);
        model.addAttribute("meta", meta);
        model.addAttribute("naturezas", NaturezaMeta.values());
        model.addAttribute("activeMenu", "metas");
        return "metas/form";
    }

    @PostMapping("/salvar")
    public String salvar(MetaFinanceira meta, Principal principal, RedirectAttributes redirectAttributes) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (meta.getResponsavel() == null) {
            meta.setResponsavel(user);
        }
        
        if (meta.getIcone() == null || meta.getIcone().equals("target")) {
            switch (meta.getNatureza()) {
                case VIAGEM -> meta.setIcone("palmtree");
                case CARRO -> meta.setIcone("car");
                case CASA -> meta.setIcone("home");
                case APOSENTADORIA -> meta.setIcone("trending-up");
                case RESERVA_EMERGENCIA -> meta.setIcone("shield-check");
                case EDUCACAO -> meta.setIcone("graduation-cap");
                default -> meta.setIcone("target");
            }
        }

        metaRepository.save(meta);
        redirectAttributes.addFlashAttribute("successMessage", "Meta salva com sucesso! Continue firme.");
        return "redirect:/app/financeiro/metas";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        metaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Meta removida.");
        return "redirect:/app/financeiro/metas";
    }

    // ── AI Suggestion Endpoints ───────────────────────────────────────────────

    @PostMapping("/ai-suggest")
    @ResponseBody
    public ResponseEntity<List<MetaSugestaoDTO>> aiSuggest(Principal principal) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().build();

        List<Investimento> investimentos = investimentoRepository.findByResponsavel(user);
        List<MetaFinanceira> metasExistentes = metaRepository.findByResponsavel(user);

        BigDecimal totalInvestido = investimentos.stream()
            .map(i -> i.getValorAtual() != null ? i.getValorAtual() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder ctx = new StringBuilder();
        ctx.append("- Rendimento mensal: €").append(user.getOrcamentoMensal()).append("\n");
        ctx.append("- Perfil financeiro: ").append(user.getTipoPerfilFinanceiro()).append("\n");
        ctx.append("- Meta de poupança mensal: ").append(user.getMetaPoupancaMensal()).append("%\n");
        ctx.append("- Total em investimentos: €").append(totalInvestido).append("\n");
        ctx.append("- Número de investimentos ativos: ").append(investimentos.size()).append("\n");
        ctx.append("- Metas financeiras já definidas: ").append(metasExistentes.size()).append("\n");
        if (!metasExistentes.isEmpty()) {
            ctx.append("- Metas existentes: ");
            metasExistentes.forEach(m -> ctx.append(m.getTitulo()).append(" (€").append(m.getValorAlvo()).append("), "));
        }

        String rawJson = openRouterService.suggestGoals(ctx.toString());

        // Strip markdown fences if the model wrapped the JSON
        rawJson = rawJson.trim();
        if (rawJson.startsWith("```")) {
            rawJson = rawJson.replaceAll("(?s)^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        try {
            List<MetaSugestaoDTO> sugestoes = objectMapper.readValue(rawJson,
                new TypeReference<List<MetaSugestaoDTO>>() {});
            return ResponseEntity.ok(sugestoes);
        } catch (Exception e) {
            log.error("[MetaController] Failed to parse AI response: {}", e.getMessage());
            log.error("[MetaController] Raw response: {}", rawJson);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/ai-criar")
    public String aiCriar(@RequestParam String titulo,
                           @RequestParam BigDecimal valorAlvo,
                           @RequestParam BigDecimal aporteMensal,
                           @RequestParam int prazoMeses,
                           @RequestParam NaturezaMeta natureza,
                           @RequestParam(required = false) String justificativa,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/app/financeiro/metas";

        String icone = switch (natureza) {
            case VIAGEM -> "palmtree";
            case CARRO -> "car";
            case CASA -> "home";
            case APOSENTADORIA -> "trending-up";
            case RESERVA_EMERGENCIA -> "shield-check";
            case EDUCACAO -> "graduation-cap";
            default -> "target";
        };

        MetaFinanceira nova = MetaFinanceira.builder()
            .titulo(titulo)
            .valorAlvo(valorAlvo)
            .valorAtual(BigDecimal.ZERO)
            .natureza(natureza)
            .icone(icone)
            .prazo(LocalDate.now().plusMonths(prazoMeses))
            .status("EM_ANDAMENTO")
            .isGeradoPeloSistema(true)
            .responsavel(user)
            .build();

        metaRepository.save(nova);
        redirectAttributes.addFlashAttribute("successMessage",
            "✨ Meta criada pela IA: " + nova.getTitulo());
        return "redirect:/app/financeiro/metas";
    }
}

