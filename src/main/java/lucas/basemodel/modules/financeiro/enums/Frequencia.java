package lucas.basemodel.modules.financeiro.enums;

public enum Frequencia {
    AVULSA("Avulsa"),
    SEMANAL("Semanal"),
    MENSAL("Mensal"),
    ANUAL("Anual");

    private final String descricao;

    Frequencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
