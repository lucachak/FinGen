package lucas.basemodel.modules.financeiro.enums;

public enum EscopoTransacao {
    CASA("Casa"),
    PESSOAL("Pessoal"),
    NEGOCIO("Empresa (CNPJ)");

    private final String descricao;

    EscopoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
