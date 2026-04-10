package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lucas.basemodel.modules.wealth.enums.AssetCondition;

@Entity
@Table(name = "wealth_vehicles")
@PrimaryKeyJoinColumn(name = "asset_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class VehicleAsset extends Asset {

    private String make;
    private String model;
    private Integer manufactureYear;
    private Integer mileage;

    @Enumerated(EnumType.STRING)
    private AssetCondition vehicleCondition;

    private String location;
}
