package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, UUID> {
    List<MetaFinanceira> findByResponsavel(User responsavel);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByResponsavelAndIsGeradoPeloSistemaTrue(User responsavel);
}
