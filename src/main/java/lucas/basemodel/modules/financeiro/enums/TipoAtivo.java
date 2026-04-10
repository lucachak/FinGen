package lucas.basemodel.modules.financeiro.enums;

import lombok.Getter;

@Getter
public enum TipoAtivo {
    ACOES("Ações", "trending-up"),
    FII("FIIs", "building-2"),
    BDR("BDRs", "globe"),
    ETF("ETFs", "layers"),
    CRYPTO("Criptomoedas", "coins"),
    RENDA_FIXA("Renda Fixa", "lock"),
    TESOURO("Tesouro Direto", "landmark"),
    IMOVEIS("Imóveis", "home"),
    PREVIDENCIA("Previdência", "umbrella"),
    OUTROS("Outros", "more-horizontal");

    private final String descricao;
    private final String icon;

    TipoAtivo(String descricao, String icon) {
        this.descricao = descricao;
        this.icon = icon;
    }
}
