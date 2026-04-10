package lucas.basemodel.core.config;

import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Fornece o objeto 'user' (utilizador logado) a todos os templates de forma global.
 * Evita o erro "Property or field 'tipoPerfilFinanceiro' cannot be found on null".
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UsuarioRepository usuarioRepository;

    @ModelAttribute("user")
    public User addCurrentUserToModel(Principal principal) {
        if (principal != null) {
            return usuarioRepository.findByEmail(principal.getName()).orElse(null);
        }
        return null;
    }
}
