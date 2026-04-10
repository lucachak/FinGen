package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.*;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.enums.TransportePrincipal;
import lucas.basemodel.modules.user.User;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "configuracoes_financeiras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal rendaMensalEstimada = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstrategiaDistribuicao estrategiaDistribuicao = EstrategiaDistribuicao.MANUAL;

    @Enumerated(EnumType.STRING)
    private SituacaoMoradia situacaoMoradia;

    @Column(precision = 15, scale = 2)
    private BigDecimal valorMoradia;

    @Enumerated(EnumType.STRING)
    private TransportePrincipal transportePrincipal;

    @Builder.Default
    private boolean possuiDividasAtivas = false;

    @Builder.Default
    private boolean possuiDependentes = false;

    @Builder.Default
    private int numeroDependentes = 0;
}
