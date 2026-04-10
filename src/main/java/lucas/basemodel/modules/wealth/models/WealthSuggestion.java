package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.enums.SuggestionSeverity;
import lucas.basemodel.modules.wealth.enums.SuggestionType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wealth_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WealthSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private SuggestionSeverity severity;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime dismissedAt;
}
