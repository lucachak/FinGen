package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.*;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "metas_financeiras")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaFinanceira {
    @Id
    @GeneratedValue
    private UUID id;

    private String titulo;
    
    @Builder.Default
    private BigDecimal valorAlvo = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal valorAtual = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NaturezaMeta natureza = NaturezaMeta.OUTROS;
    
    @Builder.Default
    private String icone = "target";
    
    private LocalDate prazo;
    @Builder.Default
    private String status = "EM_ANDAMENTO"; // EM_ANDAMENTO, CONCLUIDO
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User responsavel;

    @Builder.Default
    private Boolean isGeradoPeloSistema = false;
}
