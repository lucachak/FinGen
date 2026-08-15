package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, UUID> {
    
    List<TransacaoRecorrente> findByAutomacaoAtivaTrueAndDiaVencimento(Integer diaVencimento);
    
    List<TransacaoRecorrente> findByUsuario(lucas.basemodel.modules.user.User usuario);
    Optional<TransacaoRecorrente> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    
}
