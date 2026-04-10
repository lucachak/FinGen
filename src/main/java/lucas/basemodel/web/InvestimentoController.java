package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.TipoAtivo;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.financeiro.services.MercadoService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/app/wealth/investimentos")
public class InvestimentoController {

    private final InvestimentoRepository investimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MercadoService mercadoService;

    public InvestimentoController(InvestimentoRepository investimentoRepository, UsuarioRepository usuarioRepository, MercadoService mercadoService) {
        this.investimentoRepository = investimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mercadoService = mercadoService;
    }

    @GetMapping({"", "/"})
    public String listar(Model model, Principal principal) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            List<Investimento> investimentos = investimentoRepository.findByResponsavel(user);
            model.addAttribute("investimentos", investimentos);

            // Calcular Totais
            BigDecimal totalAportado = investimentos.stream()
                .map(Investimento::getValorAportado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalAtual = investimentos.stream()
                .map(Investimento::getValorAtual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal roiTotal = BigDecimal.ZERO;
            if (totalAportado != null && totalAportado.compareTo(BigDecimal.ZERO) > 0) {
                roiTotal = totalAtual.subtract(totalAportado)
                    .divide(totalAportado, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }

            model.addAttribute("totalAportado", totalAportado);
            model.addAttribute("totalAtual", totalAtual);
            model.addAttribute("roiTotal", roiTotal);
        }
        model.addAttribute("activeMenu", "investimentos");
        return "investimentos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("investimento", new Investimento());
        model.addAttribute("tipos", TipoAtivo.values());
        model.addAttribute("activeMenu", "investimentos");
        return "investimentos/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable UUID id, Model model) {
        Investimento investimento = investimentoRepository.findById(id).orElse(null);
        model.addAttribute("investimento", investimento);
        model.addAttribute("tipos", TipoAtivo.values());
        model.addAttribute("activeMenu", "investimentos");
        return "investimentos/form";
    }

    @PostMapping("/salvar")
    public String salvar(Investimento investimento, Principal principal) {
        if (investimento.getResponsavel() == null) {
            User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
            investimento.setResponsavel(user);
        }
        if (investimento.getDataAtualizacao() == null) {
            investimento.setDataAtualizacao(java.time.LocalDate.now());
        }
        investimentoRepository.save(investimento);
        return "redirect:/app/wealth/investimentos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable UUID id) {
        investimentoRepository.deleteById(id);
        return "redirect:/app/wealth/investimentos";
    }

    @PostMapping("/sync")
    public String syncMarketPrices(RedirectAttributes redirectAttributes) {
        try {
            mercadoService.atualizarCotacoes();
            redirectAttributes.addFlashAttribute("successMessage", "Cotações do mercado atualizadas com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao buscar cotações do mercado: " + e.getMessage());
        }
        return "redirect:/app/wealth/investimentos";
    }
}
