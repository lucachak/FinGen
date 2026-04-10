package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenRouterServiceTest {

    @InjectMocks
    private OpenRouterService openRouterService;

    @Mock
    private RestTemplate restTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Since OpenRouterService creates RestTemplate internally, we replace it using Reflection for testing.
        ReflectionTestUtils.setField(openRouterService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(openRouterService, "openRouterApiKey", "test-key");

        testUser = new User();
        testUser.setNomeCompleto("Teste User");
    }

    @Test
    void chat_When429TooManyRequests_ReturnsFriendlyMessage() {
        // Arrange
        List<Map<String, String>> history = Collections.emptyList();
        
        HttpClientErrorException tooManyRequestsException = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, new byte[0], Charset.defaultCharset()
        );

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(tooManyRequestsException);

        // Act
        String response = openRouterService.chat(history, testUser, java.util.Collections.emptyList());

        // Assert
        assertTrue(response.contains("temporariamente sobrecarregada") || response.contains("limite de requisições"), 
            "The service should return a friendly Portuguese error message regarding rate limits, instead of failing or throwing.");
    }
}
