package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.*;
import lucas.basemodel.modules.financeiro.enums.Frequencia;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.GrupoRecorrencia;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.user.User;

import lucas.basemodel.core.config.DeterministicEncryptionConverter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transacoes_recorrentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransacaoRecorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EscopoTransacao escopo = EscopoTransacao.PESSOAL;

    // Optional reference if a specific generic account is tied to it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private Conta conta;

    @Column(nullable = false)
    @Convert(converter = DeterministicEncryptionConverter.class)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrupoRecorrencia grupo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequencia frequencia;

    @Column(nullable = false)
    private Integer diaVencimento;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorBase;

    @Builder.Default
    @Column(nullable = false)
    private Boolean automacaoAtiva = true;

}
