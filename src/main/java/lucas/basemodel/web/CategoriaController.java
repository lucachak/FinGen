package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.enums.NaturezaCategoria;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.services.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/app/financeiro/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping({"", "/"})
    public String listarCategorias(Model model) {
        model.addAttribute("activeMenu", "gestao"); // Mantém o menu "Gestão da Casa" ativo
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "categorias/lista";
    }

    @GetMapping("/nova")
    public String novaCategoria(Model model) {
        model.addAttribute("activeMenu", "gestao");

        // Sugere uma cor padrão para facilitar
        Categoria novaCategoria = new Categoria();
        novaCategoria.setCorHexadecimal("#2563eb");

        model.addAttribute("naturezas", NaturezaCategoria.values());
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

        return "categorias/form";
    }

    @PostMapping("/salvar")
    public String salvarCategoria(Categoria categoria) {
        categoriaService.salvar(categoria);
        return "redirect:/app/financeiro/categorias";
    }
}