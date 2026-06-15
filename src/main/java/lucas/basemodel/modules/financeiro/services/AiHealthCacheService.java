package lucas.basemodel.modules.financeiro.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mantém o status de disponibilidade do microserviço Python em cache,
 * eliminando o HTTP síncrono bloqueante em cada page request.
 * Atualizado a cada 30 segundos em background.
 */
@Service
@Slf4j
public class AiHealthCacheService {

    private final AtomicBoolean online = new AtomicBoolean(false);
    private final RestTemplate restTemplate;

    @Value("${python.microservice.url:http://127.0.0.1:8000}")
    private String pythonBaseUrl;

    public AiHealthCacheService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** Retorna status cacheado — nunca bloqueia o request thread. */
    public boolean isOnline() {
        return online.get();
    }

    @PostConstruct
    public void checkOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelay = 30_000)
    public void refresh() {
        try {
            var response = restTemplate.getForEntity(pythonBaseUrl + "/", String.class);
            online.set(response.getStatusCode().is2xxSuccessful());
        } catch (Exception e) {
            online.set(false);
            log.debug("AI microservice offline: {}", e.getMessage());
        }
    }
}
