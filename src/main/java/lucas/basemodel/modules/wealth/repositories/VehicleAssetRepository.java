package lucas.basemodel.modules.wealth.repositories;

import lucas.basemodel.modules.wealth.models.VehicleAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleAssetRepository extends JpaRepository<VehicleAsset, UUID> {
}
