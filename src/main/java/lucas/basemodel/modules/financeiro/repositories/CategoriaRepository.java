package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findFirstByNomeIgnoreCase(String nome);
    Optional<Categoria> findByNomeIgnoreCaseAndEscopo(String nome, EscopoTransacao escopo);
    boolean existsByNomeIgnoreCaseAndEscopoAndIdNot(String nome, EscopoTransacao escopo, Long id);
    List<Categoria> findByEscopoOrderByNomeAsc(EscopoTransacao escopo);
    List<Categoria> findAllByOrderByEscopoAscNomeAsc();
}
