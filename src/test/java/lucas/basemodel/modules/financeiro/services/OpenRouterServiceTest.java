package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenRouterServiceTest {

    private AiModelCatalog catalog;
    private RestTemplate restTemplate;
    private OpenRouterService service;
    private User user;

    @BeforeEach
    void setUp() {
        catalog = mock(AiModelCatalog.class);
        when(catalog.fallbackModel()).thenReturn("google/gemma-3-27b-it:free");
        when(catalog.selectionFor(any())).thenReturn(new AiModelSelection("meta-llama/llama-3.3-70b-instruct:free", "google/gemma-3-27b-it:free"));

        service = new OpenRouterService(catalog, "test-key", "https://openrouter.ai/api/v1", "http://localhost:8081");
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        user = new User();
        user.setNomeCompleto("Teste User");
    }

    private static ResponseEntity<Map> reply(String content) {
        return ResponseEntity.ok(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
    }

    private static HttpClientErrorException clientError(HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), null, new byte[0], Charset.defaultCharset());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void sendsTheUsersChosenModel() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(reply("olá"));

        String answer = service.chat(Collections.emptyList(), user, Collections.emptyList());

        assertEquals("olá", answer);
        ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://openrouter.ai/api/v1/chat/completions"), request.capture(), eq(Map.class));
        Map<String, Object> body = (Map<String, Object>) request.getValue().getBody();
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", body.get("model"));
        assertEquals("Bearer test-key", request.getValue().getHeaders().getFirst("Authorization"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void rateLimitUsesTheFallbackModel() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(clientError(HttpStatus.TOO_MANY_REQUESTS))
                .thenReturn(reply("da reserva"));

        assertEquals("da reserva", service.chat(Collections.emptyList(), user, Collections.emptyList()));

        ArgumentCaptor<HttpEntity> requests = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).postForEntity(anyString(), requests.capture(), eq(Map.class));
        Map<String, Object> second = (Map<String, Object>) requests.getAllValues().get(1).getBody();
        assertEquals("google/gemma-3-27b-it:free", second.get("model"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void serverErrorAlsoUsesTheFallbackModel() {
        // Antes só HttpClientErrorException (4xx) acionava a reserva; 502/503 caíam no catch genérico.
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", null, new byte[0], Charset.defaultCharset()))
                .thenReturn(reply("ok"));

        assertEquals("ok", service.chat(Collections.emptyList(), user, Collections.emptyList()));
    }

    @Test
    void bothModelsFailingReturnsAFriendlyMessage() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(clientError(HttpStatus.TOO_MANY_REQUESTS));

        String response = service.chat(Collections.emptyList(), user, Collections.emptyList());

        assertTrue(response.contains("temporariamente sobrecarregada"), response);
    }

    @Test
    void chosenModelEqualToFallbackIsNotRetried() {
        when(catalog.selectionFor(any())).thenReturn(new AiModelSelection("google/gemma-3-27b-it:free", "google/gemma-3-27b-it:free"));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(clientError(HttpStatus.TOO_MANY_REQUESTS));

        service.chat(Collections.emptyList(), user, Collections.emptyList());

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void invalidKeyIsReportedWithoutRetry() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(clientError(HttpStatus.UNAUTHORIZED));

        String response = service.chat(Collections.emptyList(), user, Collections.emptyList());

        assertTrue(response.contains("OPENROUTER_API_KEY"), response);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void paidModelWithoutCreditsExplainsWhatToDo() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(clientError(HttpStatus.PAYMENT_REQUIRED));

        String response = service.chat(Collections.emptyList(), user, Collections.emptyList());

        assertTrue(response.contains("créditos"), response);
    }

    @Test
    void missingKeyDoesNotCallTheNetwork() {
        OpenRouterService noKey = new OpenRouterService(catalog, "", "https://openrouter.ai/api/v1", "http://localhost:8081");
        ReflectionTestUtils.setField(noKey, "restTemplate", restTemplate);

        assertTrue(noKey.chat(Collections.emptyList(), user, Collections.emptyList()).contains("OPENROUTER_API_KEY"));
        assertEquals("[]", noKey.suggestGoals("ctx", user));
        verify(restTemplate, times(0)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void suggestGoalsReturnsEmptyArrayOnFailure() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenThrow(clientError(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals("[]", service.suggestGoals("ctx", user));
    }
}
