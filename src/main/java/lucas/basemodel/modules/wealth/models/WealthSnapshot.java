package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lucas.basemodel.modules.user.User;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wealth_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalNetWorth;

    @Column(columnDefinition = "TEXT")
    private String breakdownJson;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
