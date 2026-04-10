package lucas.basemodel.modules.wealth.services;

import lucas.basemodel.modules.wealth.models.Asset;
import java.math.BigDecimal;

public interface AssetPricingService {
    BigDecimal calculateCurrentValue(Asset asset);
}
