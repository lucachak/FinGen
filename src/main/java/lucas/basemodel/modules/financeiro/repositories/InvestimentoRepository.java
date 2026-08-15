package lucas.basemodel.modules.financeiro.repositories;

import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface InvestimentoRepository extends JpaRepository<Investimento, UUID> {
    List<Investimento> findByResponsavel(User responsavel);
    Optional<Investimento> findByIdAndResponsavelId(UUID id, UUID responsavelId);

    @Query("SELECT DISTINCT UPPER(TRIM(i.ticker)) FROM Investimento i WHERE i.ticker IS NOT NULL AND TRIM(i.ticker) != ''")
    List<String> findDistinctTickers();
    
    @Modifying
    @Query("UPDATE Investimento i SET i.precoAtual = :precoAtual, " +
            "i.valorAtual = CASE WHEN i.quantidade IS NOT NULL AND i.quantidade > 0 " +
            "THEN i.quantidade * :precoAtual ELSE i.valorAtual END, " +
            "i.rentabilidade = :rentabilidade, i.dataAtualizacao = :dataAtualizacao " +
            "WHERE UPPER(TRIM(i.ticker)) = :ticker")
    int updateMarketDataByTicker(@Param("ticker") String ticker, @Param("precoAtual") BigDecimal precoAtual,
            @Param("rentabilidade") BigDecimal rentabilidade, @Param("dataAtualizacao") LocalDate dataAtualizacao);
}
