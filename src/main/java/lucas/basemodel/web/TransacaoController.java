package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.services.ContaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;

@Controller
@RequestMapping("/app/financeiro/transacoes")
public class TransacaoController {

    private final ContaService contaService;
    private final UsuarioRepository usuarioRepository;

    public TransacaoController(ContaService contaService, UsuarioRepository usuarioRepository) {
        this.contaService = contaService;
        this.usuarioRepository = usuarioRepository;
    }

    // Exibe a página com o Extrato Completo
    @GetMapping({"", "/"})
    public String listarTransacoes(Model model, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        // Mantém o dashboard marcado na sidebar, já que é uma extensão dele
        model.addAttribute("activeMenu", "dashboard");

        model.addAttribute("transacoes", contaService.listarHistoricoTransacoes(usuarioLogado));
        return "transacoes/lista";
    }

    // Redireciona o botão "Nova Transação" do Dashboard para o formulário que já existe!
    @GetMapping("/nova")
    public String novaTransacao() {
        return "redirect:/app/financeiro/contas/nova";
    }
}
