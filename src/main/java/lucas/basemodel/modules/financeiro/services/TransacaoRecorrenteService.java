package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteRequest;
import lucas.basemodel.modules.financeiro.dto.TransacaoRecorrenteResponse;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.TransacaoRecorrenteRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TransacaoRecorrenteService {

    private final TransacaoRecorrenteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public TransacaoRecorrenteService(TransacaoRecorrenteRepository repository, 
                                     UsuarioRepository usuarioRepository,
                                     CategoriaRepository categoriaRepository,
                                     CategoriaService categoriaService,
                                     EspacoFinanceiroService espacoFinanceiroService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
        this.espacoFinanceiroService = espacoFinanceiroService;
    }

    public List<TransacaoRecorrenteResponse> listar(String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return repository.findByUsuario(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public TransacaoRecorrenteResponse criar(TransacaoRecorrenteRequest request, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, request.getEscopo());
        Categoria categoria = categoriaService.validarNoEscopo(request.getCategoriaId(), request.getEscopo());

        TransacaoRecorrente recorrente = TransacaoRecorrente.builder()
                .titulo(request.getTitulo())
                .tipo(request.getTipo())
                .frequencia(request.getFrequencia())
                .grupo(request.getGrupo())
                .diaVencimento(request.getDiaVencimento())
                .valorBase(request.getValorBase())
                .categoria(categoria)
                .escopo(request.getEscopo())
                .usuario(user)
                .build();

        return toResponse(repository.save(recorrente));
    }

    public TransacaoRecorrenteResponse atualizar(UUID id, TransacaoRecorrenteRequest request, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        espacoFinanceiroService.validarAcesso(user, request.getEscopo());
        TransacaoRecorrente recorrente = repository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Transação recorrente não encontrada"));

        Categoria categoria = categoriaService.validarNoEscopo(request.getCategoriaId(), request.getEscopo());

        recorrente.setTitulo(request.getTitulo());
        recorrente.setTipo(request.getTipo());
        recorrente.setFrequencia(request.getFrequencia());
        recorrente.setGrupo(request.getGrupo());
        recorrente.setDiaVencimento(request.getDiaVencimento());
        recorrente.setValorBase(request.getValorBase());
        recorrente.setCategoria(categoria);
        recorrente.setEscopo(request.getEscopo());

        return toResponse(repository.save(recorrente));
    }

    public void excluir(UUID id, String email) {
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        TransacaoRecorrente recorrente = repository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação recorrente não encontrada"));
        repository.delete(recorrente);
    }

    public List<TransacaoRecorrente> listarTodosPorUsuario(User usuario) {
        return repository.findByUsuario(usuario);
    }

    public TransacaoRecorrente buscarPorId(UUID id, User usuario) {
        return repository.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);
    }

    public TransacaoRecorrente salvarParaUsuario(TransacaoRecorrente dados, User usuario) {
        espacoFinanceiroService.validarAcesso(usuario, dados.getEscopo());
        Categoria categoria = dados.getCategoria() != null
                ? categoriaService.validarNoEscopo(dados.getCategoria().getId(), dados.getEscopo())
                : null;

        TransacaoRecorrente alvo;
        if (dados.getId() == null) {
            alvo = new TransacaoRecorrente();
            alvo.setUsuario(usuario);
        } else {
            alvo = repository.findByIdAndUsuarioId(dados.getId(), usuario.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transação recorrente não encontrada"));
        }

        alvo.setTitulo(dados.getTitulo());
        alvo.setTipo(dados.getTipo());
        alvo.setFrequencia(dados.getFrequencia());
        alvo.setGrupo(dados.getGrupo());
        alvo.setDiaVencimento(dados.getDiaVencimento());
        alvo.setValorBase(dados.getValorBase());
        alvo.setAutomacaoAtiva(dados.getAutomacaoAtiva() != null ? dados.getAutomacaoAtiva() : true);
        alvo.setEscopo(dados.getEscopo());
        alvo.setCategoria(categoria);
        return repository.save(alvo);
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
