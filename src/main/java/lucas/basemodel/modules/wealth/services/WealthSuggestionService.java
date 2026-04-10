package lucas.basemodel.modules.wealth.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.enums.AssetType;
import lucas.basemodel.modules.wealth.enums.SuggestionSeverity;
import lucas.basemodel.modules.wealth.enums.SuggestionType;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.models.WealthSuggestion;
import lucas.basemodel.modules.wealth.repositories.WealthSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WealthSuggestionService {

    private final WealthSuggestionRepository suggestionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<WealthSuggestion> generateSuggestions(User user, WealthSnapshot latestSnapshot) {
        log.info("Generating suggestions for user: {}", user.getId());
        
        // Deactivate old suggestions
        List<WealthSuggestion> active = suggestionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
        active.forEach(s -> s.setActive(false));
        suggestionRepository.saveAll(active);

        List<WealthSuggestion> newSuggestions = new ArrayList<>();

        try {
            Map<String, BigDecimal> breakdown = objectMapper.readValue(
                latestSnapshot.getBreakdownJson(), 
                new TypeReference<Map<String, BigDecimal>>() {}
            );

            boolean hasNetWorth = latestSnapshot.getTotalNetWorth().compareTo(BigDecimal.ZERO) > 0;

            // Rule 1: Diversification (One asset class > 60%)
            if (hasNetWorth) {
                for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
                    BigDecimal percentage = entry.getValue()
                            .divide(latestSnapshot.getTotalNetWorth(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    
                    if (percentage.compareTo(new BigDecimal("60")) > 0) {
                        newSuggestions.add(WealthSuggestion.builder()
                                .user(user)
                                .type(SuggestionType.DIVERSIFICATION)
                                .severity(SuggestionSeverity.MEDIUM)
                                .message("O seu patrimônio está altamente concentrado em " + entry.getKey() + " (" + percentage.setScale(1, RoundingMode.HALF_UP) + "%). Considere diversificar para reduzir riscos.")
                                .build());
                    }
                }
            }

            // Rule 2: Emergency Fund (Bank Accounts < 3x Monthly Budget)
            BigDecimal liquidity = breakdown.getOrDefault(AssetType.BANK_ACCOUNT.name(), BigDecimal.ZERO);
            BigDecimal monthlyBudget = user.getOrcamentoMensal() != null ? user.getOrcamentoMensal() : new BigDecimal("3500.00");
            BigDecimal targetLiquidity = monthlyBudget.multiply(new BigDecimal("3"));
            
            if (liquidity.compareTo(targetLiquidity) < 0) {
                newSuggestions.add(WealthSuggestion.builder()
                        .user(user)
                        .type(SuggestionType.REBALANCING)
                        .severity(SuggestionSeverity.HIGH)
                        .message("Reserva de Emergência baixa. A sua liquidez atual cobre menos de 3 meses do seu orçamento mensal (" + monthlyBudget + "). Foque em acumular ativos de baixo risco.")
                                .build());
                    }

            // Rule 3: Depreciating Assets (Vehicles > 30% of Net Worth)
            if (hasNetWorth) {
                BigDecimal vehicles = breakdown.getOrDefault(AssetType.VEHICLE.name(), BigDecimal.ZERO);
                BigDecimal vehiclePercent = vehicles.divide(latestSnapshot.getTotalNetWorth(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                if (vehiclePercent.compareTo(new BigDecimal("30")) > 0) {
                    newSuggestions.add(WealthSuggestion.builder()
                            .user(user)
                            .type(SuggestionType.DEPRECIATION_ALERT)
                            .severity(SuggestionSeverity.LOW)
                            .message("Ativos depreciáveis (Veículos) representam " + vehiclePercent.setScale(1, RoundingMode.HALF_UP) + "% do seu patrimônio. Lembre-se que veículos perdem valor ao longo do tempo.")
                            .build());
                }
            }

            // Rule 4: Investor Profile Recommendation
            String profile = user.getTipoPerfilFinanceiro() != null ? user.getTipoPerfilFinanceiro().toUpperCase() : "CONSERVADOR";
            String recommendationMsg;
            if (profile.contains("AGRESSIVO") || profile.contains("ARROJADO")) {
                recommendationMsg = "Com base no seu perfil Agressivo, considere explorar Ativos de Renda Variável (Ações, Criptomoedas) para maximizar retornos a longo prazo, mantendo a sua reserva de emergência.";
            } else if (profile.contains("MODERADO")) {
                recommendationMsg = "Com base no seu perfil Moderado, sugerimos equilibrar a sua carteira dividindo os aportes entre Renda Fixa (para segurança) e Fundos/Ações (para crescimento estruturado).";
            } else { // CONSERVADOR
                recommendationMsg = "Com base no seu perfil Conservador, o foco deve ser a preservação de capital. Privilegie Certificados de Aforro, Depósitos a Prazo e Títulos de Renda Fixa.";
            }

            newSuggestions.add(WealthSuggestion.builder()
                    .user(user)
                    .type(SuggestionType.REBALANCING)
                    .severity(SuggestionSeverity.MEDIUM)
                    .message(recommendationMsg)
                    .build());

        } catch (Exception e) {
            log.error("Error generating suggestions for user: {}", user.getId(), e);
        }

        return suggestionRepository.saveAll(newSuggestions);
    }
}
