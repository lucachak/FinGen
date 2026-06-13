package lucas.basemodel.modules.admin;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("users", usuarioRepository.findAll());
        return "admin/dashboard";
    }
}
