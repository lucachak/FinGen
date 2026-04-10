package lucas.basemodel.modules.wealth.repositories;

import lucas.basemodel.modules.wealth.models.AssetValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetValuationRepository extends JpaRepository<AssetValuation, UUID> {
    List<AssetValuation> findByAssetIdOrderByCapturedAtDesc(UUID assetId);
}
