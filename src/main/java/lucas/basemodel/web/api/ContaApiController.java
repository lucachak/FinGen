package lucas.basemodel.web.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.ContaRequest;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.financeiro.services.ContaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST API for Contas (bills / transactions).
 * Mirrors all existing MVC routes under /app/financeiro/contas.
 */
@RestController
@RequestMapping("/api/v1/contas")
@RequiredArgsConstructor
public class ContaApiController {

    private final ContaService contaService;

    /**
     * GET /api/v1/contas
     * Query params: status (PENDENTE | PAGO | ATRASADO), escopo (CASA | PESSOAL |
     * NEGOCIO)
     * Returns paginated list of transactions.
     */
    @GetMapping
    public ResponseEntity<List<ContaResponse>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String escopo,
            @PageableDefault(size = 100) Pageable pageable, // Aumentado para pegar mais sem paginação no app
            Principal principal) {
        return ResponseEntity.ok(contaService.listar(principal.getName(), status, escopo, pageable).getContent());
    }

    /**
     * GET /api/v1/contas/{id}
     * Returns a single transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContaResponse> buscarPorId(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(contaService.buscarPorId(id, principal.getName()));
    }

    /**
     * POST /api/v1/contas
     * Body: multipart/form-data (ContaRequest fields + optional comprovante file)
     * Creates a new transaction.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ContaResponse> criar(
            @Valid @RequestPart("conta") ContaRequest request,
            @RequestPart(value = "comprovante", required = false) MultipartFile comprovante,
            Principal principal) {
        return ResponseEntity.ok(contaService.criar(request, comprovante, principal.getName()));
    }

    /**
     * PUT /api/v1/contas/{id}
     * Updates an existing transaction.
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ContaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestPart("conta") ContaRequest request,
            @RequestPart(value = "comprovante", required = false) MultipartFile comprovante,
            Principal principal) {
        return ResponseEntity.ok(contaService.atualizar(id, request, comprovante, principal.getName()));
    }

    /**
     * PATCH /api/v1/contas/{id}/pagar
     * Quick-pay: marks a transaction as paid.
     * Mirrors POST /app/financeiro/contas/pagar/{id}
     */
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<ContaResponse> pagar(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(contaService.pagar(id, principal.getName()));
    }

    /**
     * DELETE /api/v1/contas/{id}
     * Deletes a transaction.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Principal principal) {
        contaService.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/contas/lote
     * Batch import from AI-parsed bank statement.
     * Mirrors POST /app/financeiro/contas/salvar-lote
     * Body: { "transacoes": [ ContaRequest, ... ] }
     */
    @PostMapping("/lote")
    public ResponseEntity<List<ContaResponse>> importarLote(
            @RequestBody List<ContaRequest> transacoes,
            Principal principal) {
        return ResponseEntity.ok(contaService.importarLote(transacoes, principal.getName()));
    }
}