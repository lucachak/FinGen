package lucas.basemodel.modules.financeiro.enums;

public enum NaturezaCategoria {
    ESSENCIAL("Essencial (Sobrevivência, Moradia, Saúde)"),
    ESTILO_VIDA("Estilo de Vida (Lazer, Subscrições, Jantares)"),
    INVESTIMENTO("Investimento (Poupança, Ações, Negócios)"),
    EMERGENCIA("Reserva de Emergência"),
    DESPESA("Gastos"),
    OUTROS("Outros");

    private final String descricao;

    NaturezaCategoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}