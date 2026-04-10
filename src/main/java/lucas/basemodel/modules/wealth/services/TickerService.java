package lucas.basemodel.modules.wealth.services;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service to fetch real-time ticker prices (Stocks, BDRs, Crypto).
 */
public interface TickerService {
    Optional<BigDecimal> getPrice(String ticker);
}
