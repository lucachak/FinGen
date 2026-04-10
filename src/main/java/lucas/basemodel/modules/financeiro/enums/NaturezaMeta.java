package lucas.basemodel.modules.financeiro.enums;

public enum NaturezaMeta {
    APOSENTADORIA("Aposentadoria & Futuro"),
    VIAGEM("Viagens & Experiências"),
    CASA("Imóveis & Reforma"),
    CARRO("Veículos & Transporte"),
    RESERVA_EMERGENCIA("Reserva de Segurança"),
    EDUCACAO("Educação & Cursos"),
    OUTROS("Outros Objetivos");

    private final String descricao;

    NaturezaMeta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
