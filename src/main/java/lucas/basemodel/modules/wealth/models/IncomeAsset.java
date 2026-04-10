package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.experimental.SuperBuilder;
import lucas.basemodel.modules.wealth.enums.AssetType;

import java.math.BigDecimal;

@Entity
@Table(name = "wealth_income_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class IncomeAsset extends Asset {

    private String source; // e.g., Salary, Dividend, Bonus
    private String frequency; // Monthly, Annual

    @Builder.Default
    private boolean benefit = false; // Is this a corporate benefit (VR/VA)?
    
    private String benefitType; // VR, VA, Gympass, etc.
}
