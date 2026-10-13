package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.services.AiModelCatalog;
import lucas.basemodel.modules.financeiro.services.AiModelInfo;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Escolha do modelo de IA. Exposto para a tela web ({@code /api/ia}) e para o
 * app mobile ({@code /api/v1/ia}).
 */
@RestController
@RequestMapping({"/api/ia", "/api/v1/ia"})
public class AiModelController {

    private final AiModelCatalog catalog;
    private final UsuarioRepository usuarioRepository;

    public AiModelController(AiModelCatalog catalog, UsuarioRepository usuarioRepository) {
        this.catalog = catalog;
        this.usuarioRepository = usuarioRepository;
    }

    /** Modelos disponíveis e o que está em uso para o usuário. */
    @GetMapping("/models")
    public Map<String, Object> list(Principal principal) {
        return payload(currentUser(principal));
    }

    /**
     * Salva o modelo preferido. Corpo: {@code {"model": "vendor/model:free"}}.
     * Enviar {@code null} ou vazio volta ao padrão da plataforma.
     */
    @RequestMapping(value = "/model", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> choose(@RequestBody Map<String, String> body, Principal principal) {
        User user = currentUser(principal);
        String requested = body == null ? null : body.get("model");

        if (requested == null || requested.isBlank()) {
            user.setModeloIa(null);
        } else {
            requested = requested.trim();
            if (!catalog.isAllowed(requested)) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Modelo não disponível. Escolha um da lista.");
                return ResponseEntity.badRequest().body(error);
            }
            user.setModeloIa(requested);
        }
        usuarioRepository.save(user);
        return ResponseEntity.ok(payload(user));
    }

    private User currentUser(Principal principal) {
        return usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
    }

    private Map<String, Object> payload(User user) {
        List<AiModelInfo> models = catalog.available();
        var selection = catalog.selectionFor(user);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("selected", selection.text());
        out.put("default", catalog.defaultModel());
        out.put("custom", user.getModeloIa() != null && user.getModeloIa().equals(selection.text()));
        out.put("allowPaid", catalog.allowsPaidModels());
        out.put("models", models);
        return out;
    }
}
