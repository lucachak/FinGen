package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.services.ContaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;

@Controller
@RequestMapping("/app/financeiro/relatorios")
public class RelatorioController {

    private final ContaService contaService;
    private final UsuarioRepository usuarioRepository;

    public RelatorioController(ContaService contaService, UsuarioRepository usuarioRepository) {
        this.contaService = contaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String index(Model model, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        // Marca o menú de informes como activo na barra lateral
        model.addAttribute("activeMenu", "relatorios");

        // Enviar os datos agrupados á vista
        model.addAttribute("gastosPorMorador", contaService.obterGastosPorMoradorMesAtual(usuarioLogado));
        model.addAttribute("gastosPorCategoria", contaService.obterGastosPorCategoriaMesAtual(usuarioLogado));
        model.addAttribute("fluxoCaixa", contaService.obterFluxoCaixaUltimos6Meses(usuarioLogado));

        return "relatorios/index";
    }
}