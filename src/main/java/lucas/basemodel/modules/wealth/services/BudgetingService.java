package lucas.basemodel.modules.wealth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.enums.WealthStrategy;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetingService {

    private final UsuarioRepository userRepository;
    private final WealthService wealthService;

    /**
     * Re-calculates and updates the user's monthly budget based on their total net worth.
     */
    @Transactional
    public BigDecimal rebalanceBudget(User user) {
        if (user.getBudgetingStrategy() == WealthStrategy.MANUAL) {
            return user.getOrcamentoMensal();
        }

        WealthSnapshot latest = wealthService.getLatestSnapshot(user.getId());
        if (latest == null) {
            return user.getOrcamentoMensal(); // Keep manual if no data
        }

        BigDecimal netWorth = getNetWorthSafely(latest);
        BigDecimal monthlyIncome = calculateMonthlyIncome(user);
        BigDecimal suggestedBudget;

        // Adaptive Logic incorporating Income + Net Worth
        if (user.getBudgetingStrategy() == WealthStrategy.ADAPTIVE_CONSERVATIVE) {
            // Conservative: Focus on 50/30/20 rule, restricted by liquidity.
            // Floor is 50% of monthly income, adjusted by net worth growth.
            BigDecimal incomeBased = monthlyIncome.multiply(new BigDecimal("0.50"));
            BigDecimal assetBased = netWorth.multiply(new BigDecimal("0.04")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            suggestedBudget = incomeBased.add(assetBased.multiply(new BigDecimal("0.2"))); // Low weight on assets for conservative
        } else if (user.getBudgetingStrategy() == WealthStrategy.ADAPTIVE_AGGRESSIVE) {
            // Aggressive: Higher lifestyle spend allowed if net worth is high.
            BigDecimal incomeBased = monthlyIncome.multiply(new BigDecimal("0.70"));
            BigDecimal assetBased = netWorth.multiply(new BigDecimal("0.08")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            suggestedBudget = incomeBased.add(assetBased.multiply(new BigDecimal("0.5"))); 
        } else {
            return user.getOrcamentoMensal();
        }

        // Apply a floor/ceiling to avoid extremes (e.g. 1k to 50k range)
        BigDecimal floor = new BigDecimal("1500.00");
        BigDecimal ceiling = new BigDecimal("50000.00");
        
        suggestedBudget = suggestedBudget.max(floor).min(ceiling);

        log.info("Adaptive Budgeting: User {} - Net Worth {} - Strategy {} - New Budget {}", 
                user.getEmail(), netWorth, user.getBudgetingStrategy(), suggestedBudget);

        user.setOrcamentoMensal(suggestedBudget);
        userRepository.save(user);

        return suggestedBudget;
    }

    private BigDecimal calculateMonthlyIncome(User user) {
        List<Asset> assets = wealthService.getUserAssets(user.getId());
        BigDecimal total = BigDecimal.ZERO;
        
        for (Asset a : assets) {
            if (a instanceof lucas.basemodel.modules.wealth.models.IncomeAsset ia) {
                BigDecimal amount = ia.getEstimatedValue() != null ? ia.getEstimatedValue() : BigDecimal.ZERO;
                
                // If it's a benefit (VR/VA), we might weight it differently or add it fully
                // depending on user strategy. For now, we sum it.
                if ("ANNUAL".equalsIgnoreCase(ia.getFrequency())) {
                    amount = amount.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
                }
                total = total.add(amount);
            }
        }
        
        // If no income assets found, fallback to a safe default if user has one, 
        // or a very basic minimum for the calculation.
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : new BigDecimal("3000.00");
    }

    private BigDecimal getNetWorthSafely(WealthSnapshot snapshot) {
        if (snapshot == null || snapshot.getTotalNetWorth() == null) {
            return BigDecimal.ZERO;
        }
        return snapshot.getTotalNetWorth();
    }
}
