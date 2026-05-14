package lucas.basemodel.web.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.MetaRequest;
import lucas.basemodel.modules.financeiro.dto.MetaResponse;
import lucas.basemodel.modules.financeiro.dto.AiMetaSugestaoResponse;
import lucas.basemodel.modules.financeiro.services.MetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Metas (financial goals).
 * Mirrors all existing MVC routes under /app/financeiro/metas.
 */
@RestController
@RequestMapping("/api/v1/metas")
@RequiredArgsConstructor
public class MetaApiController {

    private final MetaService metaService;

    /**
     * GET /api/v1/metas
     * Returns all active goals for the current user.
     */
    @GetMapping
    public ResponseEntity<List<MetaResponse>> listar(Principal principal) {
        return ResponseEntity.ok(metaService.listarPorUsuario(principal.getName()));
    }

    /**
     * GET /api/v1/metas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MetaResponse> buscarPorId(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(metaService.buscarPorId(id, principal.getName()));
    }

    /**
     * POST /api/v1/metas
     * Body: {
     * "descricao": "Viagem Europa",
     * "natureza": "VIAGEM",
     * "valorAlvo": 15000.00,
     * "valorAtual": 2000.00,
     * "prazo": "2025-12-01"
     * }
     */
    @PostMapping
    public ResponseEntity<MetaResponse> criar(
            @Valid @RequestBody MetaRequest request,
            Principal principal) {
        return ResponseEntity.ok(metaService.criar(request, principal.getName()));
    }

    /**
     * PUT /api/v1/metas/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MetaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MetaRequest request,
            Principal principal) {
        return ResponseEntity.ok(metaService.atualizar(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        metaService.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/metas/ai-suggest
     * Calls OpenRouter to suggest goals based on the user's financial profile.
     * Mirrors POST /app/financeiro/metas/ai-suggest
     * Returns a list of AI-generated goal suggestions (not yet saved).
     */
    @PostMapping("/ai-suggest")
    public ResponseEntity<List<AiMetaSugestaoResponse>> sugerirViaAi(Principal principal) {
        return ResponseEntity.ok(metaService.sugerirViaAi(principal.getName()));
    }

    /**
     * POST /api/v1/metas/ai-criar
     * Converts an AI suggestion into a saved Meta entity.
     * Mirrors POST /app/financeiro/metas/ai-criar
     * Body: AiMetaSugestaoResponse (the suggestion object returned by ai-suggest)
     */
    @PostMapping("/ai-criar")
    public ResponseEntity<MetaResponse> criarDesugestaoAi(
            @RequestBody AiMetaSugestaoResponse sugestao,
            Principal principal) {
        return ResponseEntity.ok(metaService.criarDeSugestaoAi(sugestao, principal.getName()));
    }
}