package lucas.basemodel.modules.financeiro.dto;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;

import java.math.BigDecimal;

/** Soma de valores de um mês agrupada por escopo, tipo e situação (paga ou pendente). */
public record TotalMesPorEscopo(EscopoTransacao escopo, TipoTransacao tipo, Boolean paga, BigDecimal total) {
}
