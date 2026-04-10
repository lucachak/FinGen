package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {
    List<Orcamento> findByResponsavel(User responsavel);
    Optional<Orcamento> findByCategoriaAndResponsavel(Categoria categoria, User responsavel);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByResponsavelAndIsGeradoPeloSistemaTrue(User responsavel);
}
