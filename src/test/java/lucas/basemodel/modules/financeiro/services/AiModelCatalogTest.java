package lucas.basemodel.modules.financeiro.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lucas.basemodel.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiModelCatalogTest {

    private static final String CATALOG = """
            {"data": [
              {"id": "google/gemma-3-27b-it:free", "name": "Google: Gemma 3 27B (free)", "context_length": 96000,
               "pricing": {"prompt": "0", "completion": "0"},
               "architecture": {"input_modalities": ["text", "image"], "output_modalities": ["text"]}},
              {"id": "meta-llama/llama-3.3-70b-instruct:free", "name": "Meta: Llama 3.3 70B (free)", "context_length": 65000,
               "pricing": {"prompt": "0", "completion": "0"},
               "architecture": {"input_modalities": ["text"], "output_modalities": ["text"]}},
              {"id": "openai/gpt-4o-mini", "name": "OpenAI: GPT-4o mini", "context_length": 128000,
               "pricing": {"prompt": "0.00000015", "completion": "0.0000006"},
               "architecture": {"input_modalities": ["text", "image"], "output_modalities": ["text"]}},
              {"id": "vendor/image-maker", "name": "Image maker", "context_length": 0,
               "pricing": {"prompt": "0", "completion": "0"},
               "architecture": {"input_modalities": ["text"], "output_modalities": ["image"]}},
              {"id": "bad id with spaces", "name": "Broken",
               "pricing": {"prompt": "0", "completion": "0"}}
            ]}
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
    }

    private AiModelCatalog catalog(boolean allowPaid) throws Exception {
        JsonNode body = mapper.readTree(CATALOG);
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(body);
        return new AiModelCatalog(restTemplate, "https://openrouter.ai/api/v1/", "openrouter/free",
                "google/gemma-3-27b-it:free", "google/gemma-3-27b-it:free", allowPaid, Duration.ofMinutes(60));
    }

    private static User userWith(String model) {
        User user = new User();
        user.setModeloIa(model);
        return user;
    }

    @Test
    void listsOnlyFreeTextModelsByDefault() throws Exception {
        List<String> ids = catalog(false).available().stream().map(AiModelInfo::id).toList();

        assertTrue(ids.contains("google/gemma-3-27b-it:free"));
        assertTrue(ids.contains("meta-llama/llama-3.3-70b-instruct:free"));
        assertFalse(ids.contains("openai/gpt-4o-mini"), "paid models stay hidden unless allowed");
        assertFalse(ids.contains("vendor/image-maker"), "image-only output is not a chat model");
        assertFalse(ids.contains("bad id with spaces"));
    }

    @Test
    void configuredModelsAlwaysAppear() throws Exception {
        List<String> ids = catalog(false).available().stream().map(AiModelInfo::id).toList();
        assertTrue(ids.contains("openrouter/free"), "the platform default must be selectable");
    }

    @Test
    void paidModelsAppearWhenAllowed_andFreeOnesSortFirst() throws Exception {
        List<AiModelInfo> models = catalog(true).available();

        AiModelInfo paid = models.stream().filter(m -> m.id().equals("openai/gpt-4o-mini")).findFirst().orElseThrow();
        assertFalse(paid.free());
        assertTrue(paid.vision());
        assertEquals(models.size() - 1, models.indexOf(paid), "paid models come after the free ones");
    }

    @Test
    void marksVisionSupport() throws Exception {
        List<AiModelInfo> models = catalog(false).available();
        assertTrue(models.stream().filter(m -> m.id().equals("google/gemma-3-27b-it:free")).findFirst().orElseThrow().vision());
        assertFalse(models.stream().filter(m -> m.id().startsWith("meta-llama")).findFirst().orElseThrow().vision());
    }

    @Test
    void usesUsersChoiceWhenAllowed() throws Exception {
        AiModelSelection selection = catalog(false).selectionFor(userWith("meta-llama/llama-3.3-70b-instruct:free"));

        assertEquals("meta-llama/llama-3.3-70b-instruct:free", selection.text());
        assertEquals("google/gemma-3-27b-it:free", selection.vision(), "text-only model delegates images to the vision model");
    }

    @Test
    void visionModelIsTheChosenOneWhenItSeesImages() throws Exception {
        AiModelSelection selection = catalog(true).selectionFor(userWith("openai/gpt-4o-mini"));
        assertEquals("openai/gpt-4o-mini", selection.text());
        assertEquals("openai/gpt-4o-mini", selection.vision());
    }

    @Test
    void ignoresChoicesThatAreNoLongerAllowed() throws Exception {
        AiModelCatalog catalog = catalog(false);
        assertEquals("openrouter/free", catalog.selectionFor(userWith("openai/gpt-4o-mini")).text());
        assertEquals("openrouter/free", catalog.selectionFor(userWith("not a model")).text());
        assertEquals("openrouter/free", catalog.selectionFor(userWith(null)).text());
        assertEquals("openrouter/free", catalog.selectionFor(null).text());
    }

    @Test
    void validatesModelIds() {
        assertTrue(AiModelCatalog.isValidId("google/gemma-3-27b-it:free"));
        assertTrue(AiModelCatalog.isValidId("openrouter/free"));
        assertFalse(AiModelCatalog.isValidId(null));
        assertFalse(AiModelCatalog.isValidId(""));
        assertFalse(AiModelCatalog.isValidId("a b"));
        assertFalse(AiModelCatalog.isValidId("vendor/model\nX-Evil: 1"));
        assertFalse(AiModelCatalog.isValidId("../../etc/passwd"));
    }

    @Test
    void fallsBackToConfiguredModelsWhenOpenRouterIsDown() {
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenThrow(new RestClientException("down"));
        AiModelCatalog catalog = new AiModelCatalog(restTemplate, "https://openrouter.ai/api/v1", "openrouter/free",
                "google/gemma-3-27b-it:free", "qwen/qwen2.5-vl-72b-instruct:free", false, Duration.ofMinutes(60));

        List<String> ids = catalog.available().stream().map(AiModelInfo::id).toList();

        assertEquals(List.of("openrouter/free", "google/gemma-3-27b-it:free", "qwen/qwen2.5-vl-72b-instruct:free"), ids);
        assertNotNull(catalog.selectionFor(userWith(null)));
    }

    @Test
    void cachesTheCatalog() throws Exception {
        AiModelCatalog catalog = catalog(false);
        catalog.available();
        catalog.available();
        catalog.isAllowed("openrouter/free");

        verify(restTemplate, times(1)).getForObject(anyString(), eq(JsonNode.class));
    }

    @Test
    void parseRejectsNodesWithoutAValidId() throws Exception {
        assertNull(AiModelCatalog.parse(mapper.readTree("{\"name\": \"no id\"}")));
    }
}
