package lucas.basemodel.modules.financeiro.services;

import com.fasterxml.jackson.databind.JsonNode;
import lucas.basemodel.modules.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Catálogo de modelos de IA que o usuário pode escolher.
 *
 * <p>Lê a lista pública do OpenRouter e guarda em memória por {@code ai.catalog.ttl-minutes}.
 * Por padrão só expõe modelos gratuitos, porque quem paga a conta é o dono da chave:
 * defina {@code AI_ALLOW_PAID_MODELS=true} para liberar os pagos.
 *
 * <p>Se o OpenRouter estiver fora do ar, o catálogo cai para os três modelos
 * configurados (padrão, reserva e visão), então a escolha nunca quebra a tela.
 */
@Service
@lombok.extern.slf4j.Slf4j
public class AiModelCatalog {

    /** "vendor/model" com sufixo opcional (":free"). Barra qualquer outra coisa antes de ir para a API. */
    private static final Pattern MODEL_ID = Pattern.compile("^[A-Za-z0-9][\\w.\\-]{0,60}(/[\\w.\\-]{1,80})?(:[\\w.\\-]{1,30})?$");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String defaultModel;
    private final String fallbackModel;
    private final String visionModel;
    private final boolean allowPaid;
    private final Duration ttl;

    private final AtomicReference<Snapshot> cache = new AtomicReference<>();

    @Autowired
    public AiModelCatalog(@Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
                          @Value("${ai.model.default:openrouter/free}") String defaultModel,
                          @Value("${ai.model.fallback:google/gemma-3-27b-it:free}") String fallbackModel,
                          @Value("${ai.model.vision:google/gemma-3-27b-it:free}") String visionModel,
                          @Value("${ai.allow-paid-models:false}") boolean allowPaid,
                          @Value("${ai.catalog.ttl-minutes:60}") long ttlMinutes) {
        this(newRestTemplate(), baseUrl, defaultModel, fallbackModel, visionModel, allowPaid,
                Duration.ofMinutes(ttlMinutes));
    }

    AiModelCatalog(RestTemplate restTemplate, String baseUrl, String defaultModel, String fallbackModel,
                   String visionModel, boolean allowPaid, Duration ttl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultModel = defaultModel;
        this.fallbackModel = fallbackModel;
        this.visionModel = visionModel;
        this.allowPaid = allowPaid;
        this.ttl = ttl;
    }

    private static RestTemplate newRestTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }

    public String defaultModel() {
        return defaultModel;
    }

    public String fallbackModel() {
        return fallbackModel;
    }

    public String visionModel() {
        return visionModel;
    }

    public boolean allowsPaidModels() {
        return allowPaid;
    }

    /** Modelos que o usuário pode escolher: gratuitos primeiro, depois por nome. */
    public List<AiModelInfo> available() {
        return snapshot().models();
    }

    public static boolean isValidId(String id) {
        return id != null && MODEL_ID.matcher(id).matches();
    }

    public boolean isAllowed(String id) {
        return isValidId(id) && available().stream().anyMatch(m -> m.id().equals(id));
    }

    /** O modelo salvo pelo usuário se ainda for permitido; senão o padrão da plataforma. */
    public AiModelSelection selectionFor(User user) {
        String preferred = user != null ? user.getModeloIa() : null;
        String text = (preferred != null && isAllowed(preferred)) ? preferred : defaultModel;
        boolean textSeesImages = available().stream().anyMatch(m -> m.id().equals(text) && m.vision());
        return new AiModelSelection(text, textSeesImages ? text : visionModel);
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    private Snapshot snapshot() {
        Snapshot current = cache.get();
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current;
        }
        try {
            Snapshot fresh = new Snapshot(fetch(), Instant.now().plus(ttl));
            cache.set(fresh);
            return fresh;
        } catch (Exception e) {
            log.warn("[AiModelCatalog] Não foi possível ler o catálogo do OpenRouter: {}", e.getMessage());
            // Mantém a lista antiga, se houver, e tenta de novo em 1 minuto.
            List<AiModelInfo> models = current != null ? current.models() : configuredOnly();
            Snapshot degraded = new Snapshot(models, Instant.now().plus(Duration.ofMinutes(1)));
            cache.set(degraded);
            return degraded;
        }
    }

    private List<AiModelInfo> fetch() {
        JsonNode root = restTemplate.getForObject(baseUrl + "/models", JsonNode.class);
        if (root == null || !root.path("data").isArray()) {
            throw new IllegalStateException("resposta sem a lista 'data'");
        }

        Map<String, AiModelInfo> byId = new LinkedHashMap<>();
        for (JsonNode node : root.get("data")) {
            AiModelInfo model = parse(node);
            if (model != null && (model.free() || allowPaid)) {
                byId.put(model.id(), model);
            }
        }
        // Os três configurados sempre aparecem, mesmo que o catálogo mude de nome.
        for (AiModelInfo configured : configuredOnly()) {
            byId.putIfAbsent(configured.id(), configured);
        }

        List<AiModelInfo> list = new ArrayList<>(byId.values());
        list.sort(Comparator.comparing(AiModelInfo::free).reversed()
                .thenComparing(m -> m.name().toLowerCase()));
        return List.copyOf(list);
    }

    static AiModelInfo parse(JsonNode node) {
        String id = node.path("id").asText("");
        if (!isValidId(id)) {
            return null;
        }
        // Só modelos que devolvem texto servem para chat e análise.
        JsonNode outputs = node.path("architecture").path("output_modalities");
        if (outputs.isArray() && !containsText(outputs, "text")) {
            return null;
        }
        boolean vision = containsText(node.path("architecture").path("input_modalities"), "image");

        BigDecimal prompt = price(node.path("pricing").path("prompt"));
        BigDecimal completion = price(node.path("pricing").path("completion"));
        // Preço -1 é como o OpenRouter marca modelos "roteadores" de preço variável.
        boolean free = id.endsWith(":free") || id.equals("openrouter/free")
                || (prompt != null && completion != null
                && prompt.signum() == 0 && completion.signum() == 0);

        String name = node.path("name").asText(id);
        return new AiModelInfo(id, name, node.path("context_length").asInt(0), free, vision, prompt);
    }

    private static boolean containsText(JsonNode array, String value) {
        if (array == null || !array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (value.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal price(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<AiModelInfo> configuredOnly() {
        Map<String, AiModelInfo> byId = new LinkedHashMap<>();
        byId.put(defaultModel, new AiModelInfo(defaultModel, defaultModel, 0, true, false, null));
        byId.putIfAbsent(fallbackModel, new AiModelInfo(fallbackModel, fallbackModel, 0, true, false, null));
        byId.putIfAbsent(visionModel, new AiModelInfo(visionModel, visionModel, 0, true, true, null));
        return List.copyOf(byId.values());
    }

    private record Snapshot(List<AiModelInfo> models, Instant expiresAt) {
    }
}
