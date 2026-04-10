package lucas.basemodel.modules.wealth.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of TickerService using BrAPI (https://brapi.dev).
 */
@Service
@Slf4j
public class BrApiTickerService implements TickerService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://brapi.dev/api/quote/";

    @Override
    public Optional<BigDecimal> getPrice(String ticker) {
        if (ticker == null || ticker.isEmpty()) return Optional.empty();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(API_URL + ticker)
                    .toUriString();

            // Structure: { results: [ { regularMarketPrice: ... } ] }
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (!results.isEmpty()) {
                    Object priceObj = results.get(0).get("regularMarketPrice");
                    if (priceObj instanceof Number number) {
                        return Optional.of(BigDecimal.valueOf(number.doubleValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching price for ticker {}: {}", ticker, e.getMessage());
        }

        return Optional.empty();
    }
}
