package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Categoria salvar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    // Busca uma categoria pelo ID para poder editá-la
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    public Categoria buscarOuCriarPorNome(String nome) {
        return categoriaRepository.findByNome(nome)
                .orElseGet(() -> {
                    Categoria nova = new Categoria();
                    nova.setNome(nome);
                    nova.setNatureza(lucas.basemodel.modules.financeiro.enums.NaturezaCategoria.DESPESA);
                    return categoriaRepository.save(nova);
                });
    }
}