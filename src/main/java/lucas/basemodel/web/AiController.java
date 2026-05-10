package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.services.GeminiService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import lucas.basemodel.modules.financeiro.services.OpenRouterService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Arrays;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;


@Controller
@RequestMapping("/api/ia")
public class AiController {

    private final GeminiService geminiService;
    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final OpenRouterService openRouterService;

    public AiController(GeminiService geminiService, ContaRepository contaRepository, UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository, OpenRouterService openRouterService) {
        this.geminiService = geminiService;
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.openRouterService = openRouterService;
    }

    // Solves session limit bloat
    private final Map<String, List<Conta>> stagingCache = new ConcurrentHashMap<>();

    @PostMapping("/chat")
    @ResponseBody
    public java.util.Map<String, String> chatOpenRouter(@RequestBody List<Map<String, String>> messages, Principal principal) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        List<Conta> ultimasContas = contaRepository.findByResponsavelAndEscopo(user, EscopoTransacao.PESSOAL);
        String resposta = openRouterService.chat(messages, user, ultimasContas);
        return Map.of("role", "assistant", "content", resposta);
    }

    @PostMapping("/upload-extrato")
    @ResponseBody
    public java.util.Map<String, Object> uploadExtrato(@RequestParam("file") MultipartFile file, Principal principal, HttpSession session) {
        Map<String, Object> resultado = geminiService.processarExtratoIA(file, principal.getName());
        
        if ("sucesso".equals(resultado.get("status"))) {
            // Refactored from session.setAttribute to prevent memory bloat in distributed sessions
            String stagingId = UUID.randomUUID().toString();
            stagingCache.put(stagingId, (List<Conta>) resultado.get("loteDeContas"));
            session.setAttribute("staging_id", stagingId);
            resultado.put("redirect", "/app/ia/revisar");
        }
        
        return resultado;
    }

    @PostMapping("/confirmar")
    @org.springframework.transaction.annotation.Transactional
    public String confirmarImportacao(@RequestParam Map<String, String> allParams, Principal principal, HttpSession session) {
        String stagingId = (String) session.getAttribute("staging_id");
        if (stagingId == null || !stagingCache.containsKey(stagingId)) {
            return "redirect:/app/ia";
        }
        List<Conta> staging = stagingCache.get(stagingId);

        // Pre-load all categories into a Map to fix the N+1 vulnerability
        Map<String, lucas.basemodel.modules.financeiro.models.Categoria> categoryCache = categoriaRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(c -> c.getNome().toLowerCase(), c -> c, (a, b) -> a));

        List<Conta> paraSalvar = new ArrayList<>();
        
        // O form envia selected[0]=true, selected[1]=true, etc.
        for (int i = 0; i < staging.size(); i++) {
            if (allParams.containsKey("selected[" + i + "]")) {
                Conta c = staging.get(i);
                
                // Atualizar dados que podem ter sido editados no form
                String novaDesc = allParams.get("descricao[" + i + "]");
                String novaData = allParams.get("data[" + i + "]");
                String novaCat = allParams.get("categoria[" + i + "]");
                
                if (novaDesc != null) c.setDescricao(novaDesc);
                if (novaData != null) {
                    LocalDate parsedDate = LocalDate.parse(novaData);
                    c.setDataVencimento(parsedDate);
                    c.setDataPagamento(parsedDate);
                    c.setPaga(true);
                }
                
                if (novaCat != null) {
                    lucas.basemodel.modules.financeiro.models.Categoria cat = categoryCache.get(novaCat.toLowerCase());
                    if (cat != null) {
                        c.setCategoria(cat);
                    }
                }
                
                paraSalvar.add(c);
            }
        }

        if (!paraSalvar.isEmpty()) {
            contaRepository.saveAll(paraSalvar);
        }

        stagingCache.remove(stagingId);
        session.removeAttribute("staging_id");
        return "redirect:/app/dashboard?import_success=true";
    }

    @GetMapping("/consultor-pessoal")
    @ResponseBody
    public String consultorPessoal(Principal principal) {
        return geminiService.gerarPlanoInvestimentos(principal.getName());
    }

    // ==========================================
    // Endpoint para o Micro-Dashboard HTMX
    // ==========================================
    @GetMapping("/analisar-anomalias")
    public String obterInsightsAnomalias(Model model, Principal principal) {
        String respostaIA = geminiService.analisarAnomalias(principal.getName());
        model.addAttribute("lastAnalysis", respostaIA);
        return "home/fragmentos :: resultado-ia-premium";
    }

    @GetMapping("/extracao-status")
    @ResponseBody
    public String checkExtractionStatus(Principal principal) {
        return geminiService.getStatus(principal.getName());
    }

    @GetMapping("/status")
    @ResponseBody
    public java.util.Map<String, Object> checkAiStatus() {
        boolean online = geminiService.isServiceAvailable();
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("online", online);
        status.put("message", online ? "Córtex Ativo" : "Córtex Offline");
        return status;
    }
}