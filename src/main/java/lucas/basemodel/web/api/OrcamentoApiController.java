package lucas.basemodel.web.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.OrcamentoRequest;
import lucas.basemodel.modules.financeiro.dto.OrcamentoResponse;
import lucas.basemodel.modules.financeiro.services.OrcamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;

/**
 * REST API for Orçamentos (category budgets).
 * Mirrors /app/financeiro/orcamentos.
 */
@RestController
@RequestMapping("/api/v1/orcamentos")
@RequiredArgsConstructor
class OrcamentoApiController {

    private final OrcamentoService orcamentoService;

    /** GET /api/v1/orcamentos — all budgets with current consumption % */
    @GetMapping
    public ResponseEntity<List<OrcamentoResponse>> listar(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(orcamentoService.listarComConsumo(principal.getName(), escopo));
    }

    /** POST /api/v1/orcamentos — create budget */
    @PostMapping
    public ResponseEntity<OrcamentoResponse> criar(
            @Valid @RequestBody OrcamentoRequest request, Principal principal) {
        return ResponseEntity.ok(orcamentoService.criar(request, principal.getName()));
    }

    /** PUT /api/v1/orcamentos/{id} — update budget */
    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody OrcamentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(orcamentoService.atualizar(id, request, principal.getName()));
    }

    /** DELETE /api/v1/orcamentos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        orcamentoService.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/orcamentos/gerar-automatico
     * Auto-generates budgets based on last 3 months average (+10% margin).
     * Mirrors POST /app/financeiro/orcamentos/gerar-automatico
     */
    @PostMapping("/gerar-automatico")
    public ResponseEntity<List<OrcamentoResponse>> gerarAutomatico(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(orcamentoService.gerarAutomatico(principal.getName(), escopo));
    }
}
