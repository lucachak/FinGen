package lucas.basemodel.core.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DeterministicEncryptionConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionUtils.encryptDeterministic(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionUtils.decryptDeterministic(dbData);
    }
}
