package lucas.basemodel.core.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RandomEncryptionConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionUtils.encryptRandom(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionUtils.decryptRandom(dbData);
    }
}
