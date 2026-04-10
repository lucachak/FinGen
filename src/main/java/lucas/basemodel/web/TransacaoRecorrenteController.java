package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.GrupoRecorrencia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.financeiro.services.CategoriaService;
import lucas.basemodel.modules.financeiro.services.TransacaoRecorrenteService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/app/financeiro/recorrentes")
public class TransacaoRecorrenteController {

    private final TransacaoRecorrenteService recorrenteService;
    private final CategoriaService categoriaService;
    private final UsuarioRepository usuarioRepository;

    public TransacaoRecorrenteController(TransacaoRecorrenteService recorrenteService, CategoriaService categoriaService, UsuarioRepository usuarioRepository) {
        this.recorrenteService = recorrenteService;
        this.categoriaService = categoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String listarRecorrentes(Model model, Principal principal) {
        model.addAttribute("activeMenu", "automacao");
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        model.addAttribute("recorrentes", recorrenteService.listarTodosPorUsuario(usuarioLogado));
        return "recorrentes/lista";
    }

    @GetMapping("/nova")
    public String novaRecorrente(Model model) {
        model.addAttribute("activeMenu", "automacao");
        model.addAttribute("recorrente", new TransacaoRecorrente());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("tipos", TipoTransacao.values());
        model.addAttribute("grupos", GrupoRecorrencia.values());
        model.addAttribute("frequencias", Frequencia.values());
        return "recorrentes/form";
    }

    @GetMapping("/editar/{id}")
    public String editarRecorrente(@PathVariable UUID id, Model model, Principal principal) {
        model.addAttribute("activeMenu", "automacao");
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        TransacaoRecorrente recorrente = recorrenteService.buscarPorId(id, usuarioLogado);
        if (recorrente == null) return "redirect:/app/financeiro/recorrentes";

        model.addAttribute("recorrente", recorrente);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("tipos", TipoTransacao.values());
        model.addAttribute("grupos", GrupoRecorrencia.values());
        model.addAttribute("frequencias", Frequencia.values());
        return "recorrentes/form";
    }

    @PostMapping("/salvar")
    public String salvarRecorrente(@ModelAttribute("recorrente") TransacaoRecorrente recorrente, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        recorrente.setUsuario(usuarioLogado);
        recorrenteService.salvar(recorrente);
        return "redirect:/app/financeiro/recorrentes";
    }

    @PostMapping("/excluir/{id}")
    public String excluirRecorrente(@PathVariable UUID id, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        recorrenteService.excluir(id, usuarioLogado);
        return "redirect:/app/financeiro/recorrentes";
    }
}
