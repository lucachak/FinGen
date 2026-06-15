package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lucas.basemodel.modules.wealth.enums.RealEstateType;

import lucas.basemodel.core.config.RandomEncryptionConverter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "wealth_real_estate")
@PrimaryKeyJoinColumn(name = "asset_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class RealEstateAsset extends Asset {

    @Enumerated(EnumType.STRING)
    private RealEstateType realEstateType;

    @Column(precision = 19, scale = 2)
    private BigDecimal sizeM2;

    private Integer bedrooms;
    private Integer bathrooms;
    private Integer parkingSpots;

    @Convert(converter = RandomEncryptionConverter.class)
    private String address;

    @Column(precision = 19, scale = 2)
    private BigDecimal acquisitionPrice;
    private LocalDate acquisitionDate;
}
