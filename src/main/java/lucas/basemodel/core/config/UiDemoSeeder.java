package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de demonstração para avaliar telas (contas atrasadas, a vencer, pagas, receitas,
 * orçamento perto do limite, texto longo). Só roda com o perfil {@code uidemo}; nunca em produção.
 */
@Component
@Profile("uidemo")
@RequiredArgsConstructor
public class UiDemoSeeder {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ContaRepository contas;
    private final OrcamentoRepository orcamentos;
    private final MetaFinanceiraRepository metas;
    private final PasswordEncoder passwordEncoder;

    /** Depois de todos os CommandLineRunner (SetupInicial cria usuário demo e catálogo de categorias). */
    @EventListener(ApplicationReadyEvent.class)
    public void semear() {
        User demo = usuarios.findByEmail("demo@fingen.com").orElseGet(() -> {
            User novo = new User();
            novo.setUsername("Demo");
            novo.setEmail("demo@fingen.com");
            novo.setPassword(passwordEncoder.encode("demo123"));
            novo.setRole("ROLE_USER");
            return usuarios.save(novo);
        });
        if (contas.existsByResponsavelAndEscopo(demo, EscopoTransacao.CASA)) {
            return;
        }
        demo.setSetupCompleted(true);
        demo.setNomeCompleto("Marina Alves");
        demo.setOrcamentoMensal(new BigDecimal("5200.00"));
        usuarios.save(demo);

        LocalDate h = LocalDate.now();
        LocalDate dia5 = min(LocalDate.of(h.getYear(), h.getMonth(), 5), h);
        LocalDate dia10 = LocalDate.of(h.getYear(), h.getMonth(), 10);

        // mês corrente: CASA
        paga(demo, EscopoTransacao.CASA, "Aluguel", "1850.00", dia5, "Aluguel ou financiamento");
        paga(demo, EscopoTransacao.CASA, "Condomínio", "420.00", min(dia10, h), "Condomínio");
        paga(demo, EscopoTransacao.CASA, "Mercado do mês", "642.30", h.minusDays(2), "Mercado");
        pendente(demo, EscopoTransacao.CASA, "Energia elétrica, conta de " + h.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, new java.util.Locale("pt", "BR")), "187.45", h.plusDays(4), "Energia elétrica");
        pendente(demo, EscopoTransacao.CASA, "Internet residencial", "119.90", h.plusDays(9), "Internet residencial");
        pendente(demo, EscopoTransacao.CASA, "Água", "96.20", h.minusDays(3), "Água");

        // mês corrente: PESSOAL
        receita(demo, "Salário", "6200.00", dia5);
        receita(demo, "Freelance: landing page para Moura & Filhos Consultoria Ltda., entrega final", "1500.00", h.minusDays(6));
        paga(demo, EscopoTransacao.PESSOAL, "Transporte por aplicativo", "148.60", h.minusDays(1), "Transporte");
        paga(demo, EscopoTransacao.PESSOAL, "Cinema", "62.00", h.minusDays(4), "Lazer");
        pendente(demo, EscopoTransacao.PESSOAL, "Plano de saúde", "389.90", h.plusDays(6), "Saúde");
        pendente(demo, EscopoTransacao.PESSOAL, "Assinaturas (streaming e nuvem)", "74.80", h.plusDays(2), "Assinaturas");

        // meses anteriores, para o histórico
        for (int i = 1; i <= 3; i++) {
            LocalDate base = h.minusMonths(i).withDayOfMonth(5);
            paga(demo, EscopoTransacao.CASA, "Aluguel", "1850.00", base, "Aluguel ou financiamento");
            paga(demo, EscopoTransacao.CASA, "Mercado do mês", String.valueOf(580 + i * 23) + ".00", base.plusDays(8), "Mercado");
            receita(demo, "Salário", "6200.00", base);
            paga(demo, EscopoTransacao.PESSOAL, "Transporte por aplicativo", String.valueOf(120 + i * 11) + ".00",
                    base.plusDays(12), "Transporte");
        }

        meta(demo, "Reserva de emergência (6 meses de despesas)", NaturezaMeta.RESERVA_EMERGENCIA, "30000.00", "11400.00", h.plusMonths(14));
        meta(demo, "Viagem de fim de ano", NaturezaMeta.VIAGEM, "6500.00", "6500.00", h.plusMonths(2));

        orcamento(demo, "Lazer", "100.00");
        orcamento(demo, "Transporte", "160.00");
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private Categoria categoria(String nome, EscopoTransacao escopo) {
        return categorias.findByNomeIgnoreCaseAndEscopo(nome, escopo).orElse(null);
    }

    private void paga(User dono, EscopoTransacao escopo, String descricao, String valor, LocalDate pagamento, String cat) {
        salvar(dono, escopo, TipoTransacao.DESPESA, descricao, valor, pagamento, pagamento, cat);
    }

    private void pendente(User dono, EscopoTransacao escopo, String descricao, String valor, LocalDate vencimento, String cat) {
        salvar(dono, escopo, TipoTransacao.DESPESA, descricao, valor, vencimento, null, cat);
    }

    private void receita(User dono, String descricao, String valor, LocalDate data) {
        salvar(dono, EscopoTransacao.PESSOAL, TipoTransacao.RECEITA, descricao, valor, data, data, null);
    }

    private void salvar(User dono, EscopoTransacao escopo, TipoTransacao tipo, String descricao, String valor,
                        LocalDate vencimento, LocalDate pagamento, String nomeCategoria) {
        Conta c = new Conta();
        c.setDescricao(descricao);
        c.setValor(new BigDecimal(valor));
        c.setTipo(tipo);
        c.setEscopo(escopo);
        c.setDataVencimento(vencimento);
        c.setPaga(pagamento != null);
        c.setDataPagamento(pagamento);
        c.setStatus(pagamento != null ? StatusTransacao.PAGO : StatusTransacao.PENDENTE);
        c.setResponsavel(dono);
        if (nomeCategoria != null) {
            c.setCategoria(categoria(nomeCategoria, escopo));
        }
        contas.save(c);
    }

    private void meta(User dono, String titulo, NaturezaMeta natureza, String alvo, String atual, LocalDate prazo) {
        MetaFinanceira m = new MetaFinanceira();
        m.setTitulo(titulo);
        m.setNatureza(natureza);
        m.setValorAlvo(new BigDecimal(alvo));
        m.setValorAtual(new BigDecimal(atual));
        m.setPrazo(prazo);
        m.setStatus(new BigDecimal(atual).compareTo(new BigDecimal(alvo)) >= 0 ? "CONCLUIDO" : "EM_ANDAMENTO");
        m.setResponsavel(dono);
        metas.save(m);
    }

    private void orcamento(User dono, String nomeCategoria, String limite) {
        Categoria cat = categoria(nomeCategoria, EscopoTransacao.PESSOAL);
        if (cat != null) {
            orcamentos.save(Orcamento.builder().categoria(cat).responsavel(dono)
                    .limiteMensal(new BigDecimal(limite)).build());
        }
    }
}
