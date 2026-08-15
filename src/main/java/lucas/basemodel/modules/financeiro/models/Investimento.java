package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.Data;
import lucas.basemodel.modules.financeiro.enums.TipoAtivo;
import lucas.basemodel.modules.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investimentos")
@Data
public class Investimento {
    @Id
    @GeneratedValue
    private UUID id;

    private String nome;
    
    @Enumerated(EnumType.STRING)
    private TipoAtivo tipo; // Enum: ACOES, R_FIXA, CRYPTO, etc
    
    private BigDecimal valorAportado = BigDecimal.ZERO;
    private BigDecimal valorAtual = BigDecimal.ZERO;
    private BigDecimal rentabilidade; // variação diária de mercado (%)

    @Column(precision = 19, scale = 6)
    private BigDecimal quantidade;

    @Column(precision = 19, scale = 6)
    private BigDecimal precoAtual;

    private String ticker; // Símbolo da Bolsa/Crypto (ex: PETR4.SA, BTC-USD)

    @Column(name = "data_atualizacao")
    private LocalDate dataAtualizacao = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User responsavel;
}
