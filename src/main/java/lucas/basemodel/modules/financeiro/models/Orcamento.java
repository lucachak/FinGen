package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private BigDecimal limiteMensal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User responsavel;

    @Builder.Default
    private Boolean isGeradoPeloSistema = false;

    private LocalDate dataCriacao;
    private LocalDate dataAtualizacao;

    @PrePersist
    public void onPrePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDate.now();
        }
        this.dataAtualizacao = LocalDate.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.dataAtualizacao = LocalDate.now();
    }
}
