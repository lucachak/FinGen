package lucas.basemodel.modules.wealth.repositories;

import lucas.basemodel.modules.wealth.models.StockAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockAssetRepository extends JpaRepository<StockAsset, UUID> {
}
