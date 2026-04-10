package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.financeiro.repositories.TransacaoRecorrenteRepository;
import lucas.basemodel.modules.user.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TransacaoRecorrenteService {

    private final TransacaoRecorrenteRepository repository;

    public TransacaoRecorrenteService(TransacaoRecorrenteRepository repository) {
        this.repository = repository;
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
}
