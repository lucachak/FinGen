package lucas.basemodel.modules.financeiro.controllers;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.OnboardingPayloadDTO;
import lucas.basemodel.modules.financeiro.services.OnboardingService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/app/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final UsuarioRepository userRepository;
    private final OnboardingService onboardingService;

    @GetMapping
    public String showWizard(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        // Se já concluiu, redireciona para o dashboard
        if (user.isSetupCompleted()) {
            return "redirect:/app/dashboard";
        }
        
        model.addAttribute("user", user);
        return "onboarding/index";
    }

    @PostMapping("/submit")
    @ResponseBody
    public String processOnboarding(@RequestBody OnboardingPayloadDTO payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        onboardingService.processarOnboarding(user, payload);
        
        return "{\"status\": \"success\", \"redirect\": \"/app/dashboard\"}";
    }
}
