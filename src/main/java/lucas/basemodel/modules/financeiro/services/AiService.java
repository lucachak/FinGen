package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.web.dto.AiChatResponse;
import lucas.basemodel.web.dto.ConfirmarImportacaoRequest;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiService geminiService;
    private final OpenRouterService openRouterService;
    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;

    public AiChatResponse chat(String message, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        List<Conta> contas = contaRepository.findAll().stream()
                .filter(c -> c.getResponsavel() != null && c.getResponsavel().getId().equals(user.getId()))
                .toList();

        List<Map<String, String>> history = List.of(Map.of("role", "user", "content", message));
        String reply = openRouterService.chat(history, user, contas);
        
        return AiChatResponse.builder()
                .reply(reply)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<ContaResponse> processarExtrato(MultipartFile file, String email) {
        Map<String, Object> result = geminiService.processarExtratoIA(file, email);
        if (result == null || "erro".equals(result.get("status"))) {
            return new ArrayList<>();
        }
        List<Conta> contas = (List<Conta>) result.get("loteDeContas");
        if (contas == null) return new ArrayList<>();
        return contas.stream()
                .map(c -> ContaResponse.builder()
                        .id(c.getId())
                        .descricao(c.getDescricao())
                        .valor(c.getValor())
                        .dataVencimento(c.getDataVencimento())
                        .tipo(c.getTipo())
                        .status(c.getStatus())
                        .escopo(c.getEscopo())
                        .frequencia(c.getFrequencia())
                        .prioridade(c.getPrioridade())
                        .categoriaNome(c.getCategoria() != null ? c.getCategoria().getNome() : null)
                        .build())
                .toList();
    }

    public List<ContaResponse> confirmarImportacao(ConfirmarImportacaoRequest request, String email) {
        // Implementation logic
        return List.of();
    }

    public String gerarPlanoInvestimento(String email) {
        return geminiService.gerarPlanoInvestimentos(email);
    }

    public String analisarAnomalias(String email) {
        return geminiService.analisarAnomalias(email);
    }

    public Map<String, Object> getStatus() {
        return Map.of(
            "gemini", geminiService.isServiceAvailable(),
            "openRouter", true // Simplified
        );
    }
}
