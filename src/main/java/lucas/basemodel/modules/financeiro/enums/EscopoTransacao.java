package lucas.basemodel.modules.financeiro.enums;

public enum EscopoTransacao {
    CASA("Despesa Partilhada da Casa"),
    PESSOAL("Finanças Pessoais"),
    NEGOCIO("Business");

    private final String descricao;

    EscopoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}