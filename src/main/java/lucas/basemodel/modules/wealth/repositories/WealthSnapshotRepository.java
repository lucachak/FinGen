package lucas.basemodel.modules.wealth.repositories;

import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WealthSnapshotRepository extends JpaRepository<WealthSnapshot, UUID> {
    List<WealthSnapshot> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
