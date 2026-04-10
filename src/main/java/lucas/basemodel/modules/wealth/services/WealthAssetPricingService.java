package lucas.basemodel.modules.wealth.services;

import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.wealth.enums.AssetCondition;
import lucas.basemodel.modules.wealth.enums.WealthStrategy;
import lucas.basemodel.modules.wealth.models.*;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
public class WealthAssetPricingService implements AssetPricingService {

    private final TickerService tickerService;

    @Autowired
    public WealthAssetPricingService(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @Override
    public BigDecimal calculateCurrentValue(Asset asset) {
        if (asset instanceof VehicleAsset v) {
            return calculateVehicleValue(v);
        } else if (asset instanceof RealEstateAsset re) {
            return calculateRealEstateValue(re);
        } else if (asset instanceof StockAsset s) {
            return calculateStockValue(s);
        } else if (asset instanceof BankAccountAsset ba) {
            return ba.getBalance();
        }
        return asset.getEstimatedValue() != null ? asset.getEstimatedValue() : BigDecimal.ZERO;
    }

    private BigDecimal calculateVehicleValue(VehicleAsset v) {
        // Base depreciation by age (10% per year, max 80%)
        BigDecimal baseValue = v.getEstimatedValue() != null ? v.getEstimatedValue() : new BigDecimal("30000.00");
        int age = Math.max(0, LocalDate.now().getYear() - v.getManufactureYear());
        
        double ageDepreciation = Math.min(0.10 * age, 0.80);
        
        // Additional depreciation by mileage (e.g., extra 2% for every 20k km over 50k)
        double mileageDepreciation = 0.0;
        if (v.getMileage() != null && v.getMileage() > 50000) {
            mileageDepreciation = Math.min(((v.getMileage() - 50000) / 20000) * 0.02, 0.15);
        }
        
        // Condition multiplier
        double conditionFactor = switch (v.getVehicleCondition()) {
            case EXCELLENT -> 1.0;
            case GOOD -> 0.95;
            case FAIR -> 0.85;
            case POOR -> 0.60;
            default -> 0.90;
        };

        BigDecimal depreciated = baseValue
                .multiply(BigDecimal.valueOf(1 - ageDepreciation))
                .multiply(BigDecimal.valueOf(1 - mileageDepreciation))
                .multiply(BigDecimal.valueOf(conditionFactor));
        
        return depreciated.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRealEstateValue(RealEstateAsset re) {
        // Price per m2 logic
        BigDecimal pricePerM2 = new BigDecimal("2500.00"); // Mocked avg price
        return re.getSizeM2().multiply(pricePerM2).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStockValue(StockAsset s) {
        // Try real price first
        Optional<BigDecimal> realPrice = tickerService.getPrice(s.getTicker());
        
        BigDecimal price = realPrice.orElseGet(() -> {
            log.warn("Falling back to mock price for ticker: {}", s.getTicker());
            return switch (s.getTicker().toUpperCase()) {
                case "AAPL" -> new BigDecimal("180.00");
                case "PETR4" -> new BigDecimal("35.00");
                case "BTC" -> new BigDecimal("65000.00");
                default -> new BigDecimal("100.00");
            };
        });
        
        return s.getQuantity().multiply(price).setScale(2, RoundingMode.HALF_UP);
    }
}
