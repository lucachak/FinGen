package lucas.basemodel.modules.financeiro.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.financeiro.dto.OnboardingPayloadDTO;
import lucas.basemodel.modules.financeiro.services.OnboardingService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/app/onboarding")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<Map<String, String>> processOnboarding(
            @RequestBody OnboardingPayloadDTO payload, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            onboardingService.processarOnboarding(user, payload);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "redirect", "/app/dashboard"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage() != null
                    ? ex.getMessage()
                    : "Os dados enviados estão inválidos.";
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", message));
        } catch (Exception ex) {
            log.error("Falha ao concluir onboarding", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Não foi possível concluir o onboarding. Tente novamente."));
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.warn("Payload inválido recebido no onboarding: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Os dados enviados estão inválidos. Revise os campos e tente novamente."));
    }
}
