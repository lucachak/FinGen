package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conversa e sugestão de metas via OpenRouter.
 *
 * <p>O modelo vem da escolha do usuário (ver {@link AiModelCatalog}). Se ele estiver
 * indisponível, tenta o modelo de reserva configurado em {@code ai.model.fallback}.
 */
@Service
@lombok.extern.slf4j.Slf4j
public class OpenRouterService {

    /** Respostas HTTP em que vale tentar o modelo de reserva. */
    private static final Set<Integer> RETRYABLE = Set.of(400, 404, 408, 429, 500, 502, 503, 504);

    private final AiModelCatalog catalog;
    private final String apiKey;
    private final String baseUrl;
    private final String referer;
    private final RestTemplate restTemplate;

    public OpenRouterService(AiModelCatalog catalog,
                             @Value("${openrouter.api.key:}") String apiKey,
                             @Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
                             @Value("${app.base-url:http://localhost:8081}") String referer) {
        this.catalog = catalog;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.referer = referer;

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(List<Map<String, String>> conversationHistory, User user, List<Conta> contasAtuais) {
        if (!isConfigured()) {
            return "Erro: A chave de API do OpenRouter não foi configurada. Defina a variável de ambiente OPENROUTER_API_KEY.";
        }

        BigDecimal rec = BigDecimal.ZERO;
        BigDecimal des = BigDecimal.ZERO;
        StringBuilder historicoGastos = new StringBuilder();
        int maxContas = 15;

        if (contasAtuais != null) {
            for (Conta c : contasAtuais) {
                if (c.getTipo() == TipoTransacao.RECEITA) rec = rec.add(c.getValor());
                else des = des.add(c.getValor());
            }

            // Exemplo das transações no rodapé (mais recentes)
            for (int i = Math.max(0, contasAtuais.size() - maxContas); i < contasAtuais.size(); i++) {
                Conta c = contasAtuais.get(i);
                historicoGastos.append("- ").append(c.getDescricao())
                        .append(" (").append(c.getTipo()).append("): € ").append(c.getValor());
                if (c.getCategoria() != null) historicoGastos.append(" [").append(c.getCategoria().getNome()).append("]");
                historicoGastos.append("\n");
            }
        }

        String perfilFinanceiro = user.getTipoPerfilFinanceiro() != null ? user.getTipoPerfilFinanceiro() : "Desconhecido";
        String orcamento = user.getOrcamentoMensal() != null ? user.getOrcamentoMensal().toString() : "Não definido";

        String systemPrompt = "Você é o 'Consultor FinGen', um Especialista Financeiro de elite trabalhando isolado na plataforma FinGen. " +
                "Responda em Português do Brasil de forma profissional, direta e acolhedora. " +
                "Use formatação em HTML básico (tags <br>, <strong>, <em>, <ul>) SEMPRE que responder; nunca responda blocos markdown ou ```html. " +
                "\n\n--- INFORMAÇÕES DO CLIENTE (" + user.getNomeCompleto() + ") ---" +
                "\nPerfil Analítico: " + perfilFinanceiro +
                "\nOrçamento Mensal: € " + orcamento +
                "\nEntradas Mapeadas (Receitas do Período): € " + rec +
                "\nGastos Totais Mapeados (Despesas): € " + des +
                "\nÚltimas Transacões Listadas (Até 15 limites):\n" + historicoGastos +
                "\n\nUse os dados dinâmicos das transações discretamente para dar conselhos exatos do patamar financeiro. Não jogue esses números explicitamente soltos se não for interactivamente questionado.";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(conversationHistory);

        return complete(catalog.selectionFor(user).text(), messages, 0.7, 1024,
                "A rede Neural está temporariamente sobrecarregada. Por favor, tente novamente em alguns instantes.",
                "Não consegui gerar uma resposta.");
    }

    // ── Sugestão de Metas Financeiras (retorna JSON estruturado) ──────────────
    public String suggestGoals(String financialContext, User user) {
        if (!isConfigured()) {
            return "[]";
        }

        String systemPrompt = "Você é um consultor financeiro especialista. " +
                "Com base nos dados financeiros fornecidos, sugira EXATAMENTE 3 metas financeiras personalizadas. " +
                "Responda SOMENTE com um array JSON válido, sem texto adicional, markdown, ou blocos de código. " +
                "O formato de cada objeto deve ser exatamente: " +
                "[{\"titulo\":\"string\",\"valorAlvo\":number,\"aporteMensal\":number,\"prazoMeses\":number," +
                "\"natureza\":\"APOSENTADORIA|VIAGEM|CASA|CARRO|RESERVA_EMERGENCIA|EDUCACAO|OUTROS\"," +
                "\"justificativa\":\"string max 120 chars\"}]";

        String userMessage = "Dados financeiros do utilizador:\n" + financialContext +
                "\n\nGere 3 metas financeiras concretas e realistas. Responda apenas com o array JSON, sem markdown.";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));

        // Em caso de falha devolve "[]", que o chamador já trata como "sem sugestões".
        return complete(catalog.selectionFor(user).text(), messages, 0.7, 1024, "[]", "[]");
    }

    // ── Chamada com modelo escolhido e reserva ────────────────────────────────

    private String complete(String model, List<Map<String, String>> messages, double temperature, int maxTokens,
                            String overloadedMessage, String emptyMessage) {
        String fallback = catalog.fallbackModel();
        try {
            return call(model, messages, temperature, maxTokens, emptyMessage);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.error("[OpenRouter] Modelo {} falhou com HTTP {}", model, status);

            if (status == 401) {
                return "Erro de autenticação: verifique se a chave OPENROUTER_API_KEY é válida e está activa.";
            }
            if (status == 402) {
                return "Este modelo exige créditos na conta do OpenRouter. Escolha um modelo gratuito.";
            }
            if (RETRYABLE.contains(status) && !fallback.equals(model)) {
                log.info("[OpenRouter] Tentando modelo de reserva: {}", fallback);
                try {
                    return call(fallback, messages, temperature, maxTokens, emptyMessage);
                } catch (Exception fallbackEx) {
                    log.error("[OpenRouter] Reserva {} também falhou: {}", fallback, fallbackEx.getMessage());
                    return overloadedMessage;
                }
            }
            return RETRYABLE.contains(status) ? overloadedMessage
                    : "Ocorreu um erro na comunicação com a IA (HTTP " + status + ").";
        } catch (ResourceAccessException e) {
            log.error("[OpenRouter] Timeout ou erro de rede: {}", e.getMessage());
            return "Não foi possível contactar a rede Neural. Verifique a sua ligação e tente novamente.";
        } catch (Exception e) {
            log.error("[OpenRouter] Erro inesperado: ", e);
            return "Ocorreu um erro inesperado ao comunicar com a IA.";
        }
    }

    @SuppressWarnings("unchecked")
    private String call(String model, List<Map<String, String>> messages, double temperature, int maxTokens,
                        String emptyMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", referer);
        headers.set("X-Title", "FinGen");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.get("choices") instanceof List<?> choices && !choices.isEmpty()
                && choices.get(0) instanceof Map<?, ?> first
                && first.get("message") instanceof Map<?, ?> message
                && message.get("content") instanceof String content && !content.isBlank()) {
            return content;
        }
        return emptyMessage;
    }
}
