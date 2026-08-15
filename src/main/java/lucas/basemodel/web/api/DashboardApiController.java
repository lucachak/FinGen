package lucas.basemodel.web.api;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.services.DashboardService;
import lucas.basemodel.modules.financeiro.dto.DashboardSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;

/**
 * REST API for dashboard data.
 * Replaces the HTMX fragment at GET /app/dashboard/chart-data.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard/summary
     * Returns all KPIs, free cash flow, and upcoming bills for the current month.
     *
     * Example response:
     * {
     * "gastosCasa": 1200.00,
     * "gastosPessoal": 450.00,
     * "gastosNegocio": 300.00,
     * "freeCashFlow": 550.00,
     * "totalReceitas": 5000.00,
     * "totalDespesas": 4450.00,
     * "patrimonioLiquido": 87000.00
     * }
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal.getName(), escopo));
    }

    /**
     * GET /api/v1/dashboard/chart-data
     * Returns doughnut chart data (spending by category) and
     * patrimony history line chart (last 12 months).
     *
     * Example response:
     * {
     * "doughnut": [
     * { "categoria": "Alimentação", "valor": 800.00 },
     * { "categoria": "Transporte", "valor": 300.00 }
     * ],
     * "patrimonioHistory": [
     * { "mes": "2024-06", "valor": 82000.00 },
     * { "mes": "2024-07", "valor": 85000.00 }
     * ]
     * }
     */
    @GetMapping("/chart-data")
    public ResponseEntity<?> getChartData(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(dashboardService.getChartData(principal.getName(), escopo));
    }

    /**
     * GET /api/v1/dashboard/proximas-contas
     * Returns the next upcoming pending bills.
     */
    @GetMapping("/proximas-contas")
    public ResponseEntity<?> getProximasContas(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(dashboardService.getProximasContas(principal.getName(), escopo));
    }

    /**
     * GET /api/v1/dashboard/transacoes-recentes
     * Returns the last 5 paid transactions.
     */
    @GetMapping("/transacoes-recentes")
    public ResponseEntity<?> getTransacoesRecentes(
            @RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Principal principal) {
        return ResponseEntity.ok(dashboardService.getTransacoesRecentes(principal.getName(), escopo));
    }
}
