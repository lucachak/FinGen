package lucas.basemodel.modules.wealth.repositories;

import lucas.basemodel.modules.wealth.models.WealthSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WealthSuggestionRepository extends JpaRepository<WealthSuggestion, UUID> {
    List<WealthSuggestion> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);
}
