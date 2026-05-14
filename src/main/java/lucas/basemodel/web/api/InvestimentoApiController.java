package lucas.basemodel.web.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.InvestimentoPortfolioResponse;
import lucas.basemodel.modules.financeiro.dto.InvestimentoRequest;
import lucas.basemodel.modules.financeiro.dto.InvestimentoResponse;
import lucas.basemodel.modules.financeiro.services.InvestimentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investimentos")
@RequiredArgsConstructor
public class InvestimentoApiController {

    private final InvestimentoService investimentoService;

    /** GET /api/v1/investimentos — full portfolio with total ROI */
    @GetMapping
    public ResponseEntity<InvestimentoPortfolioResponse> listar(Principal principal) {
        return ResponseEntity.ok(investimentoService.getPortfolio(principal.getName()));
    }

    /**
     * POST /api/v1/investimentos — add asset to portfolio
     * Body: { "nome": "Tesouro IPCA+", "tipo": "TESOURO_DIRETO",
     * "valorAportado": 5000.00, "valorAtual": 5300.00 }
     */
    @PostMapping
    public ResponseEntity<InvestimentoResponse> criar(
            @Valid @RequestBody InvestimentoRequest request, Principal principal) {
        return ResponseEntity.ok(investimentoService.criar(request, principal.getName()));
    }

    /** PUT /api/v1/investimentos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<InvestimentoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody InvestimentoRequest request,
            Principal principal) {
        return ResponseEntity.ok(investimentoService.atualizar(id, request, principal.getName()));
    }

    /** DELETE /api/v1/investimentos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        investimentoService.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    /** POST /api/v1/investimentos/sync — sync market prices via MercadoService */
    @PostMapping("/sync")
    public ResponseEntity<Void> syncCotacoes(Principal principal) {
        investimentoService.syncCotacoes(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
