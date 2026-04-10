package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MercadoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoService.class);

    private final InvestimentoRepository investimentoRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.api.url:http://localhost:8000}")
    private String aiApiUrl;

    public MercadoService(InvestimentoRepository investimentoRepository) {
        this.investimentoRepository = investimentoRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    // Run every 2 hours (Fixed delay of 7,200,000 ms)
    @Scheduled(fixedDelay = 7200000)
    @Transactional
    public void atualizarCotacoes() {
        logger.info("Iniciando rotina de atualização de cotações (Sincronização em Lote)...");

        // Buscar diretamente na DB apenas os tickers únicos (Escalável: O(1) impacto de RAM)
        List<String> tickersUnicos = investimentoRepository.findDistinctTickers();

        if (tickersUnicos.isEmpty()) {
            logger.info("Nenhum ticker com mercado encontrado. Ignorando atualização de API.");
            return;
        }

        logger.info("Tickers detectados para sincronização: {}", tickersUnicos);

        String url = aiApiUrl + "/api/market/quotes";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tickers", tickersUnicos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && "sucesso".equals(body.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> cotacoes = (Map<String, Map<String, Object>>) body.get("cotacoes");
                
                int totalAtualizados = 0;
                LocalDate dataHoje = LocalDate.now();

                // Loop pelas cotações para fazer Bulk Updates no banco SQL
                for (Map.Entry<String, Map<String, Object>> entry : cotacoes.entrySet()) {
                    String ticker = entry.getKey();
                    Map<String, Object> dados = entry.getValue();

                    if (dados.containsKey("preco_atual")) {
                        BigDecimal preco = BigDecimal.valueOf(((Number) dados.get("preco_atual")).doubleValue());
                        BigDecimal variacao = BigDecimal.ZERO;

                        if (dados.containsKey("variacao_pct")) {
                            variacao = BigDecimal.valueOf(((Number) dados.get("variacao_pct")).doubleValue());
                        }

                        // Atualiza todos os investimentos com esse ticker diretamente em DB
                        int atualizados = investimentoRepository.updateMarketDataByTicker(ticker, preco, variacao, dataHoje);
                        totalAtualizados += atualizados;
                    }
                }
                
                if (totalAtualizados > 0) {
                    logger.info("Sincronização concluída! {} posições atualizadas via JPQL.", totalAtualizados);
                }
            } else {
                logger.warn("A API de Inteligência AI retornou falha na cotação: {}", body);
            }
        } catch (Exception e) {
            logger.error("Erro na comunicação com servidor Python de cotações: {}", e.getMessage());
        }
    }
}
