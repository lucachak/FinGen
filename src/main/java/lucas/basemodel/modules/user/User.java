package lucas.basemodel.modules.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.wealth.enums.WealthStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ConfiguracaoFinanceira configuracaoFinanceira;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role;

    private String nomeCompleto;
    private String telefone;
    private String fotoPerfil;

    @Builder.Default
    private boolean ativo = true;

    @Builder.Default
    private boolean emailVerificado = false;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal orcamentoMensal = new BigDecimal("3500.00");

    @Builder.Default
    private String tipoPerfilFinanceiro = "CONSERVADOR";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private WealthStrategy budgetingStrategy = WealthStrategy.MANUAL;

    @Builder.Default
    @Column(name = "setup_completed")
    private boolean setupCompleted = false;

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal metaPoupancaMensal = new BigDecimal("20.00");

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal tetoGastosEssenciais = new BigDecimal("50.00");

    private LocalDateTime ultimoAcesso;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    private LocalDateTime atualizadoEm;

    public void setConfiguracaoFinanceira(ConfiguracaoFinanceira configuracaoFinanceira) {
        this.configuracaoFinanceira = configuracaoFinanceira;
        if (configuracaoFinanceira != null) {
            configuracaoFinanceira.setUser(this);
        }
    }
}