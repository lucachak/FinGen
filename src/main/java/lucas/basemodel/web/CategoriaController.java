package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.NaturezaCategoria;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.services.CategoriaService;
import lucas.basemodel.modules.financeiro.services.EspacoFinanceiroService;
import lucas.basemodel.modules.user.UsuarioRepository;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/app/financeiro/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final UsuarioRepository usuarioRepository;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public CategoriaController(CategoriaService categoriaService, UsuarioRepository usuarioRepository,
                               EspacoFinanceiroService espacoFinanceiroService) {
        this.categoriaService = categoriaService;
        this.usuarioRepository = usuarioRepository;
        this.espacoFinanceiroService = espacoFinanceiroService;
    }

    @GetMapping({"", "/"})
    public String listarCategorias(@RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo,
                                   Model model, Principal principal) {
        var user = usuarioRepository.findByEmail(principal.getName()).orElseThrow();
        boolean admin = "ROLE_ADMIN".equals(user.getRole());
        if (!admin) espacoFinanceiroService.validarAcesso(user, escopo);
        model.addAttribute("activeMenu", "gestao"); // Mantém o menu "Gestão da Casa" ativo
        model.addAttribute("categorias", categoriaService.listarPorEscopo(escopo));
        model.addAttribute("escopoSelecionado", escopo);
        model.addAttribute("escopos", admin ? java.util.List.of(EscopoTransacao.values())
                : espacoFinanceiroService.listarPermitidos(user));
        model.addAttribute("podeGerenciarCategorias", admin);
        return "categorias/lista";
    }

    @GetMapping("/nova")
    public String novaCategoria(@RequestParam(defaultValue = "PESSOAL") EscopoTransacao escopo, Model model) {
        model.addAttribute("activeMenu", "gestao");

        // Sugere uma cor padrão para facilitar
        Categoria novaCategoria = new Categoria();
        novaCategoria.setCorHexadecimal("#2563eb");
        novaCategoria.setEscopo(escopo);

        model.addAttribute("naturezas", NaturezaCategoria.values());
        model.addAttribute("escopos", EscopoTransacao.values());
        model.addAttribute("categoria", novaCategoria);
        return "categorias/form";
    }

    @GetMapping("/editar/{id}")
    public String editarCategoria(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "gestao");

        Categoria categoria = categoriaService.buscarPorId(id);
        if (categoria == null) {
            return "redirect:/app/financeiro/categorias";
        }

        model.addAttribute("categoria", categoria);
        model.addAttribute("naturezas", NaturezaCategoria.values());
        model.addAttribute("escopos", EscopoTransacao.values());

        return "categorias/form";
    }

    @PostMapping("/salvar")
    public String salvarCategoria(Categoria categoria) {
        Categoria salva = categoriaService.salvar(categoria);
        return "redirect:/app/financeiro/categorias?escopo=" + salva.getEscopo().name();
    }
}
