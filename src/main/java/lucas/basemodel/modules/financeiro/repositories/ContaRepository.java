package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {

        @EntityGraph(attributePaths = { "responsavel", "categoria", "asset" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel " +
                        "AND (:status IS NULL OR c.status = :status) " +
                        "AND (:escopo IS NULL OR c.escopo = :escopo)")
        Page<Conta> findForApi(
                        @Param("responsavel") User responsavel,
                        @Param("status") StatusTransacao status,
                        @Param("escopo") EscopoTransacao escopo,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "responsavel", "categoria", "asset" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel " +
                        "AND c.paga = false AND c.dataVencimento < :hoje " +
                        "AND (:escopo IS NULL OR c.escopo = :escopo)")
        Page<Conta> findOverdueForApi(
                        @Param("responsavel") User responsavel,
                        @Param("escopo") EscopoTransacao escopo,
                        @Param("hoje") LocalDate hoje,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "responsavel", "categoria", "asset" })
        java.util.Optional<Conta> findByIdAndResponsavelId(Long id, java.util.UUID responsavelId);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel " +
                        "AND c.dataVencimento >= :inicio AND c.dataVencimento < :fimExclusivo")
        List<Conta> findByResponsavelAndVencimentoNoIntervalo(@Param("responsavel") User responsavel,
                        @Param("inicio") LocalDate inicio, @Param("fimExclusivo") LocalDate fimExclusivo);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndPagaAndTipo(User responsavel, boolean isPaga, TipoTransacao tipo);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findTop5ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndPagaFalseAndDataVencimentoBeforeOrderByDataVencimentoAsc(User responsavel,
                        LocalDate data);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndPagaFalseAndDataVencimentoGreaterThanEqualOrderByDataVencimentoAsc(
                        User responsavel, LocalDate data);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndPagaFalseOrderByDataVencimentoAsc(User responsavel);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findTop100ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(User responsavel);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findByResponsavelAndEscopo(User responsavel, EscopoTransacao escopo);

        boolean existsByDescricaoAndDataVencimentoAndResponsavel(
                        String descricao, LocalDate dataVencimento, User responsavel);

        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Conta c WHERE c.responsavel = :user AND c.tipo = lucas.basemodel.modules.financeiro.enums.TipoTransacao.DESPESA AND c.categoria = :categoria AND c.dataPagamento >= :inicio AND c.dataPagamento < :fimExclusivo AND c.paga = true")
        java.math.BigDecimal sumGastosPorCategoriaMesAno(@Param("user") User user,
                        @Param("categoria") lucas.basemodel.modules.financeiro.models.Categoria categoria,
                        @Param("inicio") LocalDate inicio, @Param("fimExclusivo") LocalDate fimExclusivo);

        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        List<Conta> findTop6ByTransacaoRecorrenteAndStatusOrderByDataVencimentoDesc(
                        TransacaoRecorrente transacaoRecorrente, StatusTransacao status);

        /**
         * Query unificada — substitui as 4 chamadas separadas do ContaController.
         * Traz TODAS as contas do responsável, ordenadas por vencimento.
         * Filtros (atrasadas, a vencer, pendentes, histórico) são aplicados em memória
         * com streams.
         */
        @EntityGraph(attributePaths = { "responsavel", "categoria", "asset" })
        List<Conta> findAllByResponsavelOrderByDataVencimentoAsc(User responsavel);

        /**
         * Query batch para o fluxo de caixa — substitui 6 queries sequenciais.
         * Retorna todas as contas de um intervalo de datas, permitindo agrupamento em
         * memória.
         */
        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel " +
                        "AND (c.dataVencimento BETWEEN :inicio AND :fim OR c.dataPagamento BETWEEN :inicio AND :fim)")
        List<Conta> findByResponsavelAndPeriodo(
                        @Param("responsavel") User responsavel,
                        @Param("inicio") java.time.LocalDate inicio,
                        @Param("fim") java.time.LocalDate fim);

        /**
         * Batch sum por categorias — substitui loop N+1 no OrcamentoController.
         * Retorna a soma de despesas pagas por categoria_id no mês/ano informados.
         */
        @Query("SELECT c.categoria.id, COALESCE(SUM(c.valor), 0) FROM Conta c " +
                        "WHERE c.responsavel = :user " +
                        "AND c.tipo = lucas.basemodel.modules.financeiro.enums.TipoTransacao.DESPESA " +
                        "AND c.paga = true " +
                        "AND c.dataPagamento >= :inicio " +
                        "AND c.dataPagamento < :fimExclusivo " +
                        "AND c.categoria.id IN :categoriaIds " +
                        "GROUP BY c.categoria.id")
        List<Object[]> sumGastosPorCategoriasBatch(
                        @Param("user") User user,
                        @Param("categoriaIds") List<Long> categoriaIds,
                        @Param("inicio") LocalDate inicio,
                        @Param("fimExclusivo") LocalDate fimExclusivo);

        /**
         * Totais do mês por escopo/tipo/situação numa única query agregada.
         * Pagas entram pelo dataPagamento (não nulo); pendentes pelo dataVencimento.
         */
        @Query("SELECT new lucas.basemodel.modules.financeiro.dto.TotalMesPorEscopo(c.escopo, c.tipo, c.paga, SUM(c.valor)) " +
                        "FROM Conta c WHERE c.responsavel = :responsavel AND (" +
                        "(c.paga = true AND c.dataPagamento >= :inicio AND c.dataPagamento < :fimExclusivo) OR " +
                        "(c.paga = false AND c.dataVencimento >= :inicio AND c.dataVencimento < :fimExclusivo)) " +
                        "GROUP BY c.escopo, c.tipo, c.paga")
        List<lucas.basemodel.modules.financeiro.dto.TotalMesPorEscopo> somarMes(
                        @Param("responsavel") User responsavel,
                        @Param("inicio") LocalDate inicio,
                        @Param("fimExclusivo") LocalDate fimExclusivo);

        /** Quantidade e total de contas pendentes com vencimento em [inicio, fimExclusivo). */
        @Query("SELECT new lucas.basemodel.modules.financeiro.dto.ResumoPendente(COUNT(c), COALESCE(SUM(c.valor), 0)) " +
                        "FROM Conta c WHERE c.responsavel = :responsavel AND c.escopo = :escopo " +
                        "AND c.tipo = :tipo AND c.paga = false " +
                        "AND c.dataVencimento >= :inicio AND c.dataVencimento < :fimExclusivo")
        lucas.basemodel.modules.financeiro.dto.ResumoPendente resumirPendentes(
                        @Param("responsavel") User responsavel,
                        @Param("escopo") EscopoTransacao escopo,
                        @Param("tipo") TipoTransacao tipo,
                        @Param("inicio") LocalDate inicio,
                        @Param("fimExclusivo") LocalDate fimExclusivo);

        /** Próximas pendentes a partir de uma data, ordem vencimento/id; tipo nulo = qualquer tipo. */
        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel AND c.paga = false " +
                        "AND c.escopo = :escopo AND (:tipo IS NULL OR c.tipo = :tipo) " +
                        "AND c.dataVencimento >= :aPartirDe " +
                        "ORDER BY c.dataVencimento ASC, c.id ASC")
        List<Conta> proximasPendentes(
                        @Param("responsavel") User responsavel,
                        @Param("escopo") EscopoTransacao escopo,
                        @Param("tipo") TipoTransacao tipo,
                        @Param("aPartirDe") LocalDate aPartirDe,
                        Pageable pageable);

        /** Últimas pagas (com data de pagamento) do escopo, mais recentes primeiro. */
        @EntityGraph(attributePaths = { "responsavel", "categoria" })
        @Query("SELECT c FROM Conta c WHERE c.responsavel = :responsavel AND c.paga = true " +
                        "AND c.dataPagamento IS NOT NULL AND c.escopo = :escopo " +
                        "ORDER BY c.dataPagamento DESC, c.id DESC")
        List<Conta> pagasRecentes(
                        @Param("responsavel") User responsavel,
                        @Param("escopo") EscopoTransacao escopo,
                        Pageable pageable);

        /** Despesas por categoria (por vencimento) no intervalo: [nome|null, cor|null, soma]. */
        @Query("SELECT cat.nome, cat.corHexadecimal, SUM(c.valor) FROM Conta c LEFT JOIN c.categoria cat " +
                        "WHERE c.responsavel = :responsavel AND c.escopo = :escopo " +
                        "AND c.tipo = lucas.basemodel.modules.financeiro.enums.TipoTransacao.DESPESA " +
                        "AND c.dataVencimento >= :inicio AND c.dataVencimento < :fimExclusivo " +
                        "GROUP BY cat.nome, cat.corHexadecimal")
        List<Object[]> somarDespesasPorCategoria(
                        @Param("responsavel") User responsavel,
                        @Param("escopo") EscopoTransacao escopo,
                        @Param("inicio") LocalDate inicio,
                        @Param("fimExclusivo") LocalDate fimExclusivo);

        boolean existsByResponsavelAndEscopo(User responsavel, EscopoTransacao escopo);
}
