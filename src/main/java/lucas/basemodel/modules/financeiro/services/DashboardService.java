package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.DashboardSummaryResponse;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;

    public DashboardSummaryResponse getSummary(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        
        BigDecimal despesasPendentes = contaService.calcularTotalDespesasPendentes(user);
        BigDecimal receitas = contaService.calcularTotalEntradasMes(user);
        BigDecimal despesasPagas = contaService.calcularTotalSaidasPagasMes(user);
        
        // Simplified mapping for individual scopes
        // This is a placeholder for actual business logic from DashboardController
        return DashboardSummaryResponse.builder()
                .totalReceitas(receitas)
                .totalDespesas(despesasPagas.add(despesasPendentes))
                .totalDespesasPendentes(despesasPendentes)
                .freeCashFlow(receitas.subtract(despesasPagas).subtract(despesasPendentes))
                .build();
    }

    public Map<String, Object> getChartData(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return contaService.obterDadosDonutMesAtual(user);
    }

    public List<Conta> getProximasContas(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return contaService.listarContasAVencer(user);
    }

    public List<Conta> getTransacoesRecentes(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return contaService.listarUltimasTransacoes(user);
    }
}
