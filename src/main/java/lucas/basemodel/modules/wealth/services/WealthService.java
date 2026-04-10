package lucas.basemodel.modules.wealth.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.enums.AssetType;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.AssetValuation;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.repositories.AssetRepository;
import lucas.basemodel.modules.wealth.repositories.AssetValuationRepository;
import lucas.basemodel.modules.wealth.repositories.WealthSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WealthService {

    private final AssetRepository assetRepository;
    private final AssetValuationRepository valuationRepository;
    private final WealthSnapshotRepository snapshotRepository;
    private final AssetPricingService pricingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Asset saveAsset(Asset asset) {
        log.info("Saving asset: {} for user: {}", asset.getName(), asset.getUser().getId());
        BigDecimal initialValue = pricingService.calculateCurrentValue(asset);
        asset.setEstimatedValue(initialValue);
        asset.setLastValuationAt(LocalDateTime.now());
        return assetRepository.save(asset);
    }

    @Transactional
    public void updateAllValuations(UUID userId) {
        List<Asset> assets = assetRepository.findByUserId(userId);
        log.info("Updating valuations for {} assets of user: {}", assets.size(), userId);

        for (Asset asset : assets) {
            BigDecimal newValue = pricingService.calculateCurrentValue(asset);
            
            AssetValuation valuation = AssetValuation.builder()
                    .asset(asset)
                    .amount(newValue)
                    .currency("EUR")
                    .source("SYSTEM_AUTO")
                    .build();
            
            valuationRepository.save(valuation);
            
            asset.setEstimatedValue(newValue);
            asset.setLastValuationAt(LocalDateTime.now());
            assetRepository.save(asset);
        }
    }

    @Transactional
    public WealthSnapshot createSnapshot(User user) {
        List<Asset> assets = assetRepository.findByUserId(user.getId());
        
        BigDecimal totalNetWorth = assets.stream()
                .filter(a -> a.getType() != AssetType.INCOME)
                .map(a -> a.getEstimatedValue() != null ? a.getEstimatedValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<AssetType, BigDecimal> breakdown = assets.stream()
                .filter(a -> a.getType() != AssetType.INCOME)
                .collect(Collectors.groupingBy(
                        Asset::getType,
                        Collectors.reducing(BigDecimal.ZERO, 
                                a -> a.getEstimatedValue() != null ? a.getEstimatedValue() : BigDecimal.ZERO, 
                                BigDecimal::add)
                ));

        try {
            String breakdownJson = objectMapper.writeValueAsString(breakdown);
            WealthSnapshot snapshot = WealthSnapshot.builder()
                    .user(user)
                    .totalNetWorth(totalNetWorth)
                    .breakdownJson(breakdownJson)
                    .build();
            
            return snapshotRepository.save(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Error serializing wealth breakdown for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to create wealth snapshot", e);
        }
    }

    public List<WealthSnapshot> getHistory(UUID userId) {
        return snapshotRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public WealthSnapshot getLatestSnapshot(UUID userId) {
        List<WealthSnapshot> snapshots = snapshotRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return snapshots.isEmpty() ? null : snapshots.get(0);
    }
    
    public List<Asset> getUserAssets(UUID userId) {
        return assetRepository.findByUserId(userId);
    }

    public BigDecimal getMonthlyIncomeAssets(User user) {
        return assetRepository.findByUserId(user.getId()).stream()
                .filter(a -> a.getType() == AssetType.INCOME)
                .map(a -> a.getEstimatedValue() != null ? a.getEstimatedValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
