package lucas.basemodel.web.api;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.services.AiService;
import lucas.basemodel.web.dto.AiChatRequest;
import lucas.basemodel.web.dto.AiChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST API for AI features.
 * These routes already exist as /api/ia/* — this mirrors them cleanly
 * for the mobile client with JSON-only responses (no HTMX fragments).
 */
@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class IaApiController {

    private final AiService aiService;

    /**
     * POST /api/v1/ia/chat
     * Conversational finance chat via OpenRouter.
     * Body: { "message": "Quanto devo guardar por mês para me aposentar?" }
     * Returns: { "reply": "...", "timestamp": "..." }
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @RequestBody AiChatRequest request,
            Principal principal) {
        return ResponseEntity.ok(aiService.chat(request.getMessage(), principal.getName()));
    }

    /**
     * POST /api/v1/ia/upload-extrato
     * Sends a bank statement PDF/image to Gemini for parsing.
     * Mirrors POST /api/ia/upload-extrato
     *
     * Returns a list of staged transactions (not yet saved).
     * The client should show these for review before confirming.
     */
    @PostMapping(value = "/upload-extrato", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadExtrato(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        return ResponseEntity.ok(aiService.processarExtrato(file, principal.getName()));
    }

    /**
     * POST /api/v1/ia/confirmar
     * Confirms staged transactions from a previous upload-extrato call.
     * Saves them to the database.
     * Mirrors POST /api/ia/confirmar
     * Body: { "sessionId": "...", "transacoes": [...] }
     */
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarImportacao(
            @RequestBody lucas.basemodel.web.dto.ConfirmarImportacaoRequest request,
            Principal principal) {
        return ResponseEntity.ok(aiService.confirmarImportacao(request, principal.getName()));
    }

    /**
     * GET /api/v1/ia/consultor-pessoal
     * Generates a personalized investment plan via Gemini.
     * Mirrors GET /api/ia/consultor-pessoal
     */
    @GetMapping("/consultor-pessoal")
    public ResponseEntity<?> consultorPessoal(Principal principal) {
        return ResponseEntity.ok(aiService.gerarPlanoInvestimento(principal.getName()));
    }

    /**
     * GET /api/v1/ia/analisar-anomalias
     * Detects spending anomalies in the current month.
     * Mirrors GET /api/ia/analisar-anomalias (previously returned an HTMX
     * fragment).
     * Returns pure JSON for mobile.
     */
    @GetMapping("/analisar-anomalias")
    public ResponseEntity<?> analisarAnomalias(Principal principal) {
        return ResponseEntity.ok(aiService.analisarAnomalias(principal.getName()));
    }

    /**
     * GET /api/v1/ia/status
     * Returns health status of both AI services (Gemini + OpenRouter).
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(aiService.getStatus());
    }
}