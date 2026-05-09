package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import java.math.BigDecimal;

@Service
@lombok.extern.slf4j.Slf4j
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String PRIMARY_MODEL = "openrouter/free";
    private static final String FALLBACK_MODEL = "google/gemma-3-27b-it:free";

    private final RestTemplate restTemplate;

    public OpenRouterService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String chat(List<Map<String, String>> conversationHistory, User user, List<Conta> contasAtuais) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return "Erro: A chave de API do OpenRouter não foi configurada. Defina a variável de ambiente OPENROUTER_API_KEY.";
        }

        BigDecimal rec = BigDecimal.ZERO;
        BigDecimal des = BigDecimal.ZERO;
        StringBuilder historicoGastos = new StringBuilder();
        int transacoesProcessadas = 0;
        int max_contas = 15;
        
        if (contasAtuais != null) {
            for (Conta c : contasAtuais) {
                if (c.getTipo() == TipoTransacao.RECEITA) rec = rec.add(c.getValor());
                else des = des.add(c.getValor());
            }

            // Exemplo das transações no rodapé (mais recentes)
            for (int i = Math.max(0, contasAtuais.size() - max_contas); i < contasAtuais.size(); i++) {
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
            "\nÚltimas Transacões Listadas (Até 15 limites):\n" + historicoGastos.toString() +
            "\n\nUse os dados dinâmicos das transações discretamente para dar conselhos exatos do patamar financeiro. Não jogue esses números explicitamente soltos se não for interactivamente questionado.";

        // Monta o payload com system prompt + histórico
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(conversationHistory);

        // Tenta o modelo principal; em caso de rate-limit ou indisponibilidade usa o
        // fallback
        try {
            return callModel(PRIMARY_MODEL, messages);
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            log.error("[OpenRouter] Modelo principal falhou com HTTP {}: {}", status, e.getMessage());

            if (status == 429 || status == 503 || status == 502) {
                log.info("[OpenRouter] Tentando modelo fallback: {}", FALLBACK_MODEL);
                try {
                    return callModel(FALLBACK_MODEL, messages);
                } catch (Exception fallbackEx) {
                    log.error("[OpenRouter] Fallback também falhou: ", fallbackEx);
                    return "A rede Neural está temporariamente sobrecarregada. Por favor, tente novamente em alguns instantes.";
                }
            }

            if (status == 401) {
                return "Erro de autenticação: verifique se a chave OPENROUTER_API_KEY é válida e está activa.";
            }

            return "Ocorreu um erro na comunicação com a IA (HTTP " + status + ").";
        } catch (ResourceAccessException e) {
            log.error("[OpenRouter] Timeout ou erro de rede: ", e);
            return "Não foi possível contactar a rede Neural. Verifique a sua ligação e tente novamente.";
        } catch (Exception e) {
            log.error("[OpenRouter] Erro inesperado: ", e);
            return "Ocorreu um erro inesperado ao comunicar com a IA.";
        }
    }

    // ── Chamada isolada para um modelo específico ─────────────────────────────
    private String callModel(String modelName, List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openRouterApiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "FinGen");

        Map<String, Object> body = new HashMap<>();
        // FIX PRINCIPAL: "model" (string) — o array "models" só funciona com créditos
        // pagos
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(OPENROUTER_URL, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        return "Não consegui gerar uma resposta.";
    }

    // ── Sugestão de Metas Financeiras (retorna JSON estruturado) ──────────────
    public String suggestGoals(String financialContext) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
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

        try {
            return callModel(FALLBACK_MODEL, messages);
        } catch (HttpClientErrorException e) {
            log.error("[OpenRouter] suggestGoals failed: ", e);
            return "[]";
        } catch (Exception e) {
            log.error("[OpenRouter] suggestGoals unexpected error: ", e);
            return "[]";
        }
    }
}