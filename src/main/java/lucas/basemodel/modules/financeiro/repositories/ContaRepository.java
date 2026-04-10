package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel AND MONTH(c.dataVencimento) = :mes AND YEAR(c.dataVencimento) = :ano")
    List<Conta> findByResponsavelAndMesEAno(@Param("responsavel") User responsavel, @Param("mes") int mes, @Param("ano") int ano);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndPagaAndTipo(User responsavel, boolean isPaga, TipoTransacao tipo);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findTop5ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndPagaFalseAndDataVencimentoBeforeOrderByDataVencimentoAsc(User responsavel, LocalDate data);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndPagaFalseAndDataVencimentoGreaterThanEqualOrderByDataVencimentoAsc(User responsavel, LocalDate data);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndPagaFalseOrderByDataVencimentoAsc(User responsavel);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);
    
    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findTop100ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findByResponsavelAndEscopo(User responsavel, EscopoTransacao escopo);

    boolean existsByDescricaoAndDataVencimentoAndResponsavel(
            String descricao, LocalDate dataVencimento, User responsavel
    );

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Conta c WHERE c.responsavel = :user AND c.tipo = lucas.basemodel.modules.financeiro.enums.TipoTransacao.DESPESA AND c.categoria = :categoria AND MONTH(c.dataPagamento) = :mes AND YEAR(c.dataPagamento) = :ano AND c.paga = true")
    java.math.BigDecimal sumGastosPorCategoriaMesAno(@Param("user") User user, @Param("categoria") lucas.basemodel.modules.financeiro.models.Categoria categoria, @Param("mes") int mes, @Param("ano") int ano);

    @EntityGraph(attributePaths = {"responsavel", "categoria"})
    List<Conta> findTop6ByTransacaoRecorrenteAndStatusOrderByDataVencimentoDesc(TransacaoRecorrente transacaoRecorrente, StatusTransacao status);
}