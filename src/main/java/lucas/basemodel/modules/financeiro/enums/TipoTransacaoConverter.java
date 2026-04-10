package lucas.basemodel.modules.financeiro.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoTransacaoConverter implements AttributeConverter<TipoTransacao, String> {

    @Override
    public String convertToDatabaseColumn(TipoTransacao attribute) {
        if (attribute == null) return null;
        return attribute.name(); // Persiste como RECEITA ou DESPESA
    }

    @Override
    public TipoTransacao convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        
        switch (dbData) {
            case "ENTRADA":
            case "RECEITA":
                return TipoTransacao.RECEITA;
            case "SAIDA":
            case "DESPESA":
                return TipoTransacao.DESPESA;
            default:
                throw new IllegalArgumentException("Valor desconhecido no banco: " + dbData);
        }
    }
}
