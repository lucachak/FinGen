package lucas.basemodel.perf;

import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Gera contas sintéticas determinísticas: 36 meses para trás de {@code hoje},
 * 3 escopos, receitas e despesas, pagas e pendentes, ~80% com categoria.
 * Contas pagas sempre têm {@code dataPagamento}; vencimentos e pagamentos
 * são distintos entre si para não depender de desempate em ordenações.
 */
public final class ContaDatasetFactory {

    private static final int CATEGORIAS_POR_ESCOPO = 5;

    private ContaDatasetFactory() {}

    public static void popular(ContaRepository contas, CategoriaRepository categorias,
                               User user, int quantidade, LocalDate hoje) {
        Map<EscopoTransacao, List<Categoria>> porEscopo = new EnumMap<>(EscopoTransacao.class);
        for (EscopoTransacao escopo : EscopoTransacao.values()) {
            List<Categoria> lista = new ArrayList<>();
            for (int i = 0; i < CATEGORIAS_POR_ESCOPO; i++) {
                Categoria c = new Categoria();
                c.setNome("Perf-" + user.getId().toString().substring(0, 8) + "-" + escopo + "-" + i);
                c.setEscopo(escopo);
                c.setCorHexadecimal("#%06x".formatted(0x10_20_30 + i * 0x0F_0F_0F));
                lista.add(categorias.save(c));
            }
            porEscopo.put(escopo, lista);
        }

        EscopoTransacao[] escopos = EscopoTransacao.values();
        List<Conta> lote = new ArrayList<>(quantidade);
        for (int n = 0; n < quantidade; n++) {
            EscopoTransacao escopo = escopos[n % escopos.length];
            TipoTransacao tipo = (n % 4 == 0) ? TipoTransacao.RECEITA : TipoTransacao.DESPESA;
            boolean paga = n % 3 != 0;
            // espalha vencimentos em ~36 meses; o deslocamento por n garante datas quase únicas
            LocalDate vencimento = hoje.minusDays((n * 37L) % 1095).plusDays(n % 45 - 20);

            Conta c = new Conta();
            c.setDescricao("Perf conta " + n);
            c.setValor(new BigDecimal(10 + (n % 500)).add(new BigDecimal("0.%02d".formatted(n % 100))));
            c.setTipo(tipo);
            c.setEscopo(escopo);
            c.setDataVencimento(vencimento);
            c.setPaga(paga);
            c.setStatus(paga ? StatusTransacao.PAGO : StatusTransacao.PENDENTE);
            if (paga) {
                c.setDataPagamento(vencimento.plusDays(n % 5));
            }
            if (n % 5 != 0) {
                List<Categoria> cats = porEscopo.get(escopo);
                c.setCategoria(cats.get(n % cats.size()));
            }
            c.setResponsavel(user);
            lote.add(c);
        }
        contas.saveAll(lote);
    }
}
