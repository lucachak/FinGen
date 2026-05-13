package lucas.basemodel.web.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteRequest;
import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteResponse;
import lucas.basemodel.modules.financeiro.services.TransacaoRecorrenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recorrentes")
@RequiredArgsConstructor
public class TransacaoRecorrenteApiController {

    private final TransacaoRecorrenteService recorrenteService;

    /** GET /api/v1/recorrentes — all recurring transaction rules */
    @GetMapping
    public ResponseEntity<List<TransacaoRecorrenteResponse>> listar(Principal principal) {
        return ResponseEntity.ok(recorrenteService.listar(principal.getName()));
    }

    /**
     * POST /api/v1/recorrentes
     * Body: { "descricao": "Netflix", "valor": 45.90,
     * "tipo": "DESPESA", "frequencia": "MENSAL",
     * "diaVencimento": 15, "categoriaId": 3 }
     */
    @PostMapping
    public ResponseEntity<TransacaoRecorrenteResponse> criar(
            @Valid @RequestBody TransacaoRecorrenteRequest request, Principal principal) {
        return ResponseEntity.ok(recorrenteService.criar(request, principal.getName()));
    }

    /** PUT /api/v1/recorrentes/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<TransacaoRecorrenteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody TransacaoRecorrenteRequest request,
            Principal principal) {
        return ResponseEntity.ok(recorrenteService.atualizar(id, request, principal.getName()));
    }

    /** DELETE /api/v1/recorrentes/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Principal principal) {
        recorrenteService.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
