package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.HistoricoPatrimonio;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HistoricoPatrimonioRepository extends JpaRepository<HistoricoPatrimonio, UUID> {
    
    List<HistoricoPatrimonio> findByUsuarioOrderByDataReferenciaAsc(User usuario);
    
    Optional<HistoricoPatrimonio> findByUsuarioAndDataReferencia(User usuario, LocalDate dataReferencia);
}
