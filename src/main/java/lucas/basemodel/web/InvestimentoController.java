package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.TipoAtivo;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.financeiro.services.MercadoService;
import lucas.basemodel.modules.financeiro.services.PortfolioInsightService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/app/wealth/investimentos")
public class InvestimentoController {

    private final InvestimentoRepository investimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MercadoService mercadoService;
    private final PortfolioInsightService portfolioInsightService;

    public InvestimentoController(InvestimentoRepository investimentoRepository, UsuarioRepository usuarioRepository,
            MercadoService mercadoService, PortfolioInsightService portfolioInsightService) {
        this.investimentoRepository = investimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mercadoService = mercadoService;
        this.portfolioInsightService = portfolioInsightService;
    }

    @GetMapping({"", "/"})
    public String listar(Model model, Principal principal) {
        User user = findUser(principal);
        PortfolioInsightService.PortfolioOverview portfolio = portfolioInsightService.build(user, BigDecimal.ZERO);
        model.addAttribute("user", user);
        model.addAttribute("investimentos", investimentoRepository.findByResponsavel(user));
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("totalAportado", portfolio.totalInvested());
        model.addAttribute("totalAtual", portfolio.currentValue());
        model.addAttribute("roiTotal", portfolio.returnPercentage());
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
    public String editar(@PathVariable UUID id, Model model, Principal principal) {
        User user = findUser(principal);
        Investimento investimento = investimentoRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        model.addAttribute("investimento", investimento);
        model.addAttribute("tipos", TipoAtivo.values());
        model.addAttribute("activeMenu", "investimentos");
        return "investimentos/form";
    }

    @PostMapping("/salvar")
    public String salvar(Investimento investimento, Principal principal) {
        User user = findUser(principal);
        Investimento persisted = investimento.getId() == null
                ? new Investimento()
                : investimentoRepository.findByIdAndResponsavelId(investimento.getId(), user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        persisted.setNome(investimento.getNome());
        persisted.setTicker(investimento.getTicker());
        persisted.setTipo(investimento.getTipo());
        persisted.setQuantidade(investimento.getQuantidade());
        persisted.setValorAportado(investimento.getValorAportado());
        persisted.setValorAtual(investimento.getValorAtual());
        persisted.setResponsavel(user);
        persisted.setDataAtualizacao(java.time.LocalDate.now());
        investimentoRepository.save(persisted);
        return "redirect:/app/wealth/investimentos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable UUID id, Principal principal) {
        User user = findUser(principal);
        Investimento investimento = investimentoRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        investimentoRepository.delete(investimento);
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

    private User findUser(Principal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("Usuário não autenticado");
        }
        return usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
