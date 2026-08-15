package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.TipoAtivo;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioInsightService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final InvestimentoRepository investimentoRepository;

    public record Allocation(String key, String label, BigDecimal value, BigDecimal percentage,
            BigDecimal targetPercentage, String color) {}

    public record Position(Investimento investment, BigDecimal gain, BigDecimal returnPercentage,
            BigDecimal portfolioPercentage) {}

    public record Opportunity(String label, BigDecimal currentPercentage, BigDecimal targetPercentage,
            BigDecimal gapPercentage, BigDecimal suggestedAmount, BigDecimal progressPercentage) {}

    public record PortfolioOverview(BigDecimal totalInvested, BigDecimal currentValue, BigDecimal totalGain,
            BigDecimal returnPercentage, List<Allocation> allocations, List<Position> topPositions,
            Opportunity opportunity, boolean hasPortfolio) {}

    @Transactional(readOnly = true)
    public PortfolioOverview build(User user, BigDecimal availableForAllocation) {
        List<Investimento> investments = investimentoRepository.findByResponsavel(user);
        BigDecimal totalInvested = investments.stream().map(i -> safe(i.getValorAportado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentValue = investments.stream().map(i -> safe(i.getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGain = currentValue.subtract(totalInvested);
        BigDecimal returnPercentage = percentage(totalGain, totalInvested);

        Map<String, BigDecimal> valuesByBucket = new LinkedHashMap<>();
        investments.forEach(i -> valuesByBucket.merge(bucket(i.getTipo()), safe(i.getValorAtual()), BigDecimal::add));

        Map<String, BigDecimal> targets = targetsFor(user.getTipoPerfilFinanceiro());
        List<Allocation> allocations = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> target : targets.entrySet()) {
            BigDecimal value = valuesByBucket.getOrDefault(target.getKey(), BigDecimal.ZERO);
            allocations.add(new Allocation(target.getKey(), label(target.getKey()), value,
                    percentage(value, currentValue), target.getValue(), color(target.getKey())));
        }

        List<Position> topPositions = investments.stream()
                .sorted(Comparator.comparing((Investimento i) -> safe(i.getValorAtual())).reversed())
                .limit(5)
                .map(i -> {
                    BigDecimal gain = safe(i.getValorAtual()).subtract(safe(i.getValorAportado()));
                    return new Position(i, gain, percentage(gain, safe(i.getValorAportado())),
                            percentage(safe(i.getValorAtual()), currentValue));
                })
                .toList();

        Allocation largestGap = allocations.stream()
                .max(Comparator.comparing(a -> a.targetPercentage().subtract(a.percentage())))
                .orElse(new Allocation("OUTROS", "Outros", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, color("OUTROS")));
        BigDecimal gap = largestGap.targetPercentage().subtract(largestGap.percentage()).max(BigDecimal.ZERO);
        BigDecimal capacity = safe(availableForAllocation).max(BigDecimal.ZERO);
        BigDecimal suggestedAmount = capacity.multiply(gap).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        Opportunity opportunity = new Opportunity(largestGap.label(), largestGap.percentage(),
                largestGap.targetPercentage(), gap, suggestedAmount,
                percentage(largestGap.percentage(), largestGap.targetPercentage())
                        .max(BigDecimal.ZERO).min(ONE_HUNDRED));

        return new PortfolioOverview(totalInvested, currentValue, totalGain, returnPercentage,
                allocations, topPositions, opportunity, !investments.isEmpty());
    }

    private Map<String, BigDecimal> targetsFor(String profile) {
        String normalized = profile == null ? "CONSERVADOR" : profile.toUpperCase();
        Map<String, BigDecimal> targets = new LinkedHashMap<>();
        if ("AGRESSIVO".equals(normalized)) {
            targets.put("RENDA_FIXA", new BigDecimal("20"));
            targets.put("ACOES", new BigDecimal("30"));
            targets.put("ETF", new BigDecimal("25"));
            targets.put("FII", new BigDecimal("10"));
            targets.put("CRYPTO", new BigDecimal("10"));
            targets.put("OUTROS", new BigDecimal("5"));
        } else if ("CUSTOMIZADO".equals(normalized)) {
            targets.put("RENDA_FIXA", new BigDecimal("35"));
            targets.put("ACOES", new BigDecimal("25"));
            targets.put("ETF", new BigDecimal("15"));
            targets.put("FII", new BigDecimal("15"));
            targets.put("CRYPTO", new BigDecimal("5"));
            targets.put("OUTROS", new BigDecimal("5"));
        } else {
            targets.put("RENDA_FIXA", new BigDecimal("55"));
            targets.put("ACOES", new BigDecimal("10"));
            targets.put("ETF", new BigDecimal("15"));
            targets.put("FII", new BigDecimal("10"));
            targets.put("CRYPTO", new BigDecimal("2"));
            targets.put("OUTROS", new BigDecimal("8"));
        }
        return targets;
    }

    private String bucket(TipoAtivo type) {
        if (type == null) return "OUTROS";
        return switch (type) {
            case RENDA_FIXA, TESOURO, PREVIDENCIA -> "RENDA_FIXA";
            case ACOES, BDR -> "ACOES";
            case ETF -> "ETF";
            case FII, IMOVEIS -> "FII";
            case CRYPTO -> "CRYPTO";
            case OUTROS -> "OUTROS";
        };
    }

    private String label(String key) {
        return switch (key) {
            case "RENDA_FIXA" -> "Renda fixa";
            case "ACOES" -> "Ações e BDRs";
            case "ETF" -> "ETFs";
            case "FII" -> "FIIs e imóveis";
            case "CRYPTO" -> "Criptoativos";
            default -> "Outros";
        };
    }

    private String color(String key) {
        return switch (key) {
            case "RENDA_FIXA" -> "#0F766E";
            case "ACOES" -> "#2563EB";
            case "ETF" -> "#8B5CF6";
            case "FII" -> "#D97706";
            case "CRYPTO" -> "#DB2777";
            default -> "#64748B";
        };
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return value.multiply(ONE_HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
