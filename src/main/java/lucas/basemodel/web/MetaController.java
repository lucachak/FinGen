package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
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
public class MetaController {

    private final MetaFinanceiraRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaController(MetaFinanceiraRepository metaRepository, UsuarioRepository usuarioRepository) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
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
                // Cálculo de percentual
                BigDecimal percentual = BigDecimal.ZERO;
                if (meta.getValorAlvo().compareTo(BigDecimal.ZERO) > 0) {
                    percentual = meta.getValorAtual().multiply(new BigDecimal("100"))
                                     .divide(meta.getValorAlvo(), 2, RoundingMode.HALF_UP);
                }
                percentuais.put(meta.getId(), percentual);

                // Cálculo de tempo restante
                long meses = 0;
                if (meta.getPrazo() != null) {
                    meses = ChronoUnit.MONTHS.between(LocalDate.now(), meta.getPrazo());
                    if (meses < 0) meses = 0;
                }
                mesesRestantes.put(meta.getId(), meses);

                // Cálculo de aporte mensal necessário
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
        
        // Atribui ícone baseado na natureza se não definido
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
}
