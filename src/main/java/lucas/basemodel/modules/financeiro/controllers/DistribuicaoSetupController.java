package lucas.basemodel.modules.financeiro.controllers;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;
import lucas.basemodel.modules.financeiro.services.DistribuicaoOrcamentoService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/app/financeiro/setup")
@RequiredArgsConstructor
public class DistribuicaoSetupController {

    private final UsuarioRepository userRepository;
    private final DistribuicaoOrcamentoService distribuicaoService;

    @GetMapping
    public String showSetup(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        model.addAttribute("user", user);
        model.addAttribute("activeMenu", "setup");
        return "financeiro/setup-distribuicao";
    }

    @PostMapping
    public String processSetup(@RequestParam BigDecimal income,
                               @RequestParam EstrategiaDistribuicao strategy,
                               Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        distribuicaoService.aplicarEstrategia(user, income, strategy);
        return "redirect:/app/dashboard"; // Ou para onde for apropriado
    }
}
