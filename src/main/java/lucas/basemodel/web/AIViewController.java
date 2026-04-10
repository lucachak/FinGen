package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.services.ContaService;
import lucas.basemodel.modules.financeiro.services.GeminiService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;

import java.security.Principal;

@Controller
@RequestMapping("/app/ia")
public class AIViewController {

    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final GeminiService geminiService;
    private final CategoriaRepository categoriaRepository;

    public AIViewController(UsuarioRepository usuarioRepository, ContaService contaService, GeminiService geminiService, CategoriaRepository categoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.contaService = contaService;
        this.geminiService = geminiService;
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public String index(Model model, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        
        model.addAttribute("activeMenu", "ia");
        model.addAttribute("user", usuarioLogado);
        model.addAttribute("dadosCategoria", contaService.obterGastosPorCategoriaMesAtual(usuarioLogado));
        model.addAttribute("dadosFluxo", contaService.obterFluxoCaixaUltimos6Meses(usuarioLogado));
        model.addAttribute("aiOnline", geminiService.isServiceAvailable());
        
        return "ia/index";
    }

    @GetMapping("/revisar")
    public String revisar(Model model, Principal principal, HttpSession session) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        Object staging = session.getAttribute("staging_transactions");
        
        if (staging == null) {
            return "redirect:/app/ia";
        }
        
        model.addAttribute("user", usuarioLogado);
        model.addAttribute("stagingTransactions", staging);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("activeMenu", "ia");
        
        return "ia/revisar";
    }
}
