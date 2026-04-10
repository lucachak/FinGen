package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lucas.basemodel.modules.wealth.enums.StockAssetClass;

import java.math.BigDecimal;

@Entity
@Table(name = "wealth_stocks")
@PrimaryKeyJoinColumn(name = "asset_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class StockAsset extends Asset {

    @Column(nullable = false)
    private String ticker;

    @Column(precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal averagePurchasePrice;

    private String broker;

    @Enumerated(EnumType.STRING)
    private StockAssetClass assetClass;
}
