package lucas.basemodel.modules.wealth.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lucas.basemodel.modules.wealth.enums.BankAccountType;

import lucas.basemodel.core.config.RandomEncryptionConverter;

import java.math.BigDecimal;

@Entity
@Table(name = "wealth_bank_accounts")
@PrimaryKeyJoinColumn(name = "asset_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class BankAccountAsset extends Asset {

    @Convert(converter = RandomEncryptionConverter.class)
    private String bankName;

    @Enumerated(EnumType.STRING)
    private BankAccountType accountType;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    private String currency;
}
