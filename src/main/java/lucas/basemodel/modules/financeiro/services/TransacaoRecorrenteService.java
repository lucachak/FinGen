package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteRequest;
import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteResponse;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.TransacaoRecorrenteRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TransacaoRecorrenteService {

    private final TransacaoRecorrenteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    public TransacaoRecorrenteService(TransacaoRecorrenteRepository repository, 
                                     UsuarioRepository usuarioRepository,
                                     CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<TransacaoRecorrenteResponse> listar(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return repository.findByUsuario(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public TransacaoRecorrenteResponse criar(TransacaoRecorrenteRequest request, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        Categoria categoria = request.getCategoriaId() != null ? 
                categoriaRepository.findById(request.getCategoriaId()).orElse(null) : null;

        TransacaoRecorrente recorrente = TransacaoRecorrente.builder()
                .titulo(request.getTitulo())
                .tipo(request.getTipo())
                .frequencia(request.getFrequencia())
                .grupo(request.getGrupo())
                .diaVencimento(request.getDiaVencimento())
                .valorBase(request.getValorBase())
                .categoria(categoria)
                .usuario(user)
                .build();

        return toResponse(repository.save(recorrente));
    }

    public TransacaoRecorrenteResponse atualizar(Long id, TransacaoRecorrenteRequest request, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        // Assuming ID comes from the API as a Long but is stored as UUID
        UUID uuid = UUID.fromString(id.toString());
        TransacaoRecorrente recorrente = repository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Transação recorrente não encontrada"));

        if (!recorrente.getUsuario().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }

        Categoria categoria = request.getCategoriaId() != null ? 
                categoriaRepository.findById(request.getCategoriaId()).orElse(null) : null;

        recorrente.setTitulo(request.getTitulo());
        recorrente.setTipo(request.getTipo());
        recorrente.setFrequencia(request.getFrequencia());
        recorrente.setGrupo(request.getGrupo());
        recorrente.setDiaVencimento(request.getDiaVencimento());
        recorrente.setValorBase(request.getValorBase());
        recorrente.setCategoria(categoria);

        return toResponse(repository.save(recorrente));
    }

    public void excluir(Long id, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        UUID uuid = UUID.fromString(id.toString());
        TransacaoRecorrente recorrente = repository.findById(uuid).orElseThrow();
        if (recorrente.getUsuario().getId().equals(user.getId())) {
            repository.delete(recorrente);
        }
    }

    public List<TransacaoRecorrente> listarTodosPorUsuario(User usuario) {
        return repository.findByUsuario(usuario);
    }

    public TransacaoRecorrente buscarPorId(UUID id, User usuario) {
        TransacaoRecorrente recorrente = repository.findById(id).orElse(null);
        if (recorrente != null && recorrente.getUsuario().getId().equals(usuario.getId())) {
            return recorrente;
        }
        return null;
    }

    public TransacaoRecorrente salvar(TransacaoRecorrente transacaoRecorrente) {
        return repository.save(transacaoRecorrente);
    }

    public void excluir(UUID id, User usuario) {
        TransacaoRecorrente recorrente = buscarPorId(id, usuario);
        if (recorrente != null) {
            repository.delete(recorrente);
        }
    }

    private TransacaoRecorrenteResponse toResponse(TransacaoRecorrente t) {
        return TransacaoRecorrenteResponse.builder()
                .id(t.getId())
                .titulo(t.getTitulo())
                .tipo(t.getTipo())
                .frequencia(t.getFrequencia())
                .grupo(t.getGrupo())
                .diaVencimento(t.getDiaVencimento())
                .valorBase(t.getValorBase())
                .categoriaNome(t.getCategoria() != null ? t.getCategoria().getNome() : null)
                .automacaoAtiva(t.getAutomacaoAtiva() != null ? t.getAutomacaoAtiva() : true)
                .build();
    }
}
