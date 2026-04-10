package lucas.basemodel.modules.wealth.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.enums.AssetType;
import lucas.basemodel.modules.wealth.enums.WealthStrategy;
import lucas.basemodel.modules.wealth.models.*;
import lucas.basemodel.modules.wealth.services.BudgetingService;
import lucas.basemodel.modules.wealth.services.WealthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.TypeMismatchException;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/app/wealth/setup")
@RequiredArgsConstructor
@Slf4j
public class WealthSetupController {

    private final UsuarioRepository userRepository;
    private final WealthService wealthService;
    private final BudgetingService budgetingService;

    @GetMapping
    public String showWizard(@RequestParam(required = false, defaultValue = "1") Integer step, Model model, Principal principal) {
        return loadStep(step, model, principal);
    }

    @GetMapping("/step1")
    public String showStep1(Model model, Principal principal) {
        return loadStep(1, model, principal);
    }

    @GetMapping("/step2")
    public String showStep2(Model model, Principal principal) {
        return loadStep(2, model, principal);
    }

    @GetMapping("/step3")
    public String showStep3(Model model, Principal principal) {
        return loadStep(3, model, principal);
    }

    private String loadStep(Integer step, Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        model.addAttribute("user", user);
        model.addAttribute("step", step);
        return "wealth/setup";
    }

    @PostMapping("/step1")
    public String processStep1(@RequestParam(required = false) String assetName, 
                               @RequestParam(required = false) BigDecimal assetValue,
                               @RequestParam(required = false) AssetType assetType,
                               Principal principal) {
        if (assetName == null || assetName.isEmpty() || assetValue == null || assetType == null) {
            log.info("Step 1 skipped or empty for user: {}", principal.getName());
            return "redirect:/app/wealth/setup?step=2";
        }
        
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        
        Asset asset = switch (assetType) {
            case VEHICLE -> VehicleAsset.builder().name(assetName).estimatedValue(assetValue).type(assetType).user(user).build();
            case REAL_ESTATE -> RealEstateAsset.builder().name(assetName).estimatedValue(assetValue).type(assetType).user(user).build();
            case STOCK, CRYPTO -> StockAsset.builder().name(assetName).estimatedValue(assetValue).type(assetType).user(user).build();
            default -> BankAccountAsset.builder().name(assetName).estimatedValue(assetValue).type(assetType).user(user).build();
        };
        
        wealthService.saveAsset(asset);
        return "redirect:/app/wealth/setup/step2";
    }

    @PostMapping("/step2")
    public String processStep2(@RequestParam(required = false) String incomeSource,
                               @RequestParam(required = false) BigDecimal incomeAmount,
                               @RequestParam(required = false) BigDecimal benefitAmount,
                               @RequestParam(required = false) String benefitType,
                               Principal principal) {
        if ((incomeSource == null || incomeSource.isEmpty() || incomeAmount == null) && 
            (benefitAmount == null || benefitAmount.compareTo(BigDecimal.ZERO) == 0)) {
            log.info("Step 2 skipped or empty for user: {}", principal.getName());
            return "redirect:/app/wealth/setup/step3";
        }
        
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        
        if (incomeAmount != null && incomeAmount.compareTo(BigDecimal.ZERO) > 0) {
            IncomeAsset income = IncomeAsset.builder()
                    .name("Renda: " + (incomeSource != null ? incomeSource : "Salário"))
                    .estimatedValue(incomeAmount)
                    .source(incomeSource)
                    .frequency("MONTHLY")
                    .type(AssetType.INCOME)
                    .user(user)
                    .benefit(false)
                    .build();
            wealthService.saveAsset(income);
        }

        if (benefitAmount != null && benefitAmount.compareTo(BigDecimal.ZERO) > 0) {
            IncomeAsset benefit = IncomeAsset.builder()
                    .name("Benefício: " + benefitType)
                    .estimatedValue(benefitAmount)
                    .source("Corporate Benefit")
                    .frequency("MONTHLY")
                    .type(AssetType.INCOME)
                    .user(user)
                    .benefit(true)
                    .benefitType(benefitType)
                    .build();
            wealthService.saveAsset(benefit);
        }

        return "redirect:/app/wealth/setup/step3";
    }

    @PostMapping("/step3")
    public String processStep3(@RequestParam WealthStrategy strategy,
                               Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        user.setBudgetingStrategy(strategy);
        user.setSetupCompleted(true);
        userRepository.save(user);
        
        // Trigger initial snapshot and budget rebalance
        wealthService.createSnapshot(user);
        budgetingService.rebalanceBudget(user);
        
        return "redirect:/app/dashboard";
    }

    @ExceptionHandler({TypeMismatchException.class, NumberFormatException.class})
    public String handleTypeMismatch(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("Invalid input format in setup wizard: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "Formato numérico inválido. Por favor, use ponto (.) para casas decimais.");
        
        String requestURI = request.getRequestURI();
        if (requestURI.contains("step2")) {
            return "redirect:/app/wealth/setup/step2?error=format";
        }
        return "redirect:/app/wealth/setup/step1?error=format";
    }
}
