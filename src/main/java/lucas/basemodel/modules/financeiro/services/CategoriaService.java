package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
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
        if (categoria.getEscopo() == null) {
            throw new BadRequestException("Selecione o espaço financeiro da categoria");
        }
        if (categoria.getNome() == null || categoria.getNome().isBlank()) {
            throw new BadRequestException("Informe o nome da categoria");
        }
        categoria.setNome(categoria.getNome().trim());
        long idAtual = categoria.getId() == null ? -1L : categoria.getId();
        if (categoriaRepository.existsByNomeIgnoreCaseAndEscopoAndIdNot(
                categoria.getNome(), categoria.getEscopo(), idAtual)) {
            throw new BadRequestException("Já existe uma categoria com esse nome neste espaço");
        }
        return categoriaRepository.save(categoria);
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAllByOrderByEscopoAscNomeAsc();
    }

    public List<Categoria> listarPorEscopo(EscopoTransacao escopo) {
        return categoriaRepository.findByEscopoOrderByNomeAsc(escopo);
    }

    // Busca uma categoria pelo ID para poder editá-la
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    public Categoria buscarOuCriarPorNome(String nome) {
        return buscarOuCriarPorNome(nome, EscopoTransacao.PESSOAL);
    }

    public Categoria buscarOuCriarPorNome(String nome, EscopoTransacao escopo) {
        return categoriaRepository.findByNomeIgnoreCaseAndEscopo(nome, escopo)
                .orElseGet(() -> {
                    Categoria nova = new Categoria();
                    nova.setNome(nome);
                    nova.setEscopo(escopo);
                    nova.setNatureza(lucas.basemodel.modules.financeiro.enums.NaturezaCategoria.DESPESA);
                    return categoriaRepository.save(nova);
                });
    }

    public Categoria validarNoEscopo(Long categoriaId, EscopoTransacao escopo) {
        if (categoriaId == null) return null;
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        validarNoEscopo(categoria, escopo);
        return categoria;
    }

    public void validarNoEscopo(Categoria categoria, EscopoTransacao escopo) {
        if (categoria == null) return;
        if (escopo == null) {
            throw new BadRequestException("Selecione o espaço financeiro do lançamento");
        }
        if (categoria.getEscopo() == null || categoria.getEscopo() != escopo) {
            throw new BadRequestException("A categoria selecionada não pertence ao espaço " + escopo.getDescricao());
        }
    }
}
