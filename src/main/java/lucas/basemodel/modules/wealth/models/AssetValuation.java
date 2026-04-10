package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wealth_valuations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    private String currency;
    private String source;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime capturedAt;
}
