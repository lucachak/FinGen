package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/app/financeiro/orcamentos")
public class OrcamentoController {

    private final OrcamentoRepository orcamentoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public OrcamentoController(OrcamentoRepository orcamentoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({ "", "/" })
    public String listar(Model model, Principal principal) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            List<Orcamento> orcamentos = orcamentoRepository.findByResponsavel(user);

            Map<UUID, BigDecimal> gastosAtuais = new HashMap<>();
            Map<UUID, BigDecimal> percentuais = new HashMap<>();
            Map<UUID, String> statusRisco = new HashMap<>();

            int mesAtual = LocalDate.now().getMonthValue();
            int anoAtual = LocalDate.now().getYear();

            // OPTIMIZED: 1 batch query GROUP BY categoria (antes: 1 query por orçamento —
            // N+1)
            List<Long> categoriaIds = orcamentos.stream()
                    .filter(o -> o.getCategoria() != null)
                    .map(o -> o.getCategoria().getId())
                    .toList();

            Map<Long, BigDecimal> gastosBatch = new HashMap<>();
            if (!categoriaIds.isEmpty()) {
                List<Object[]> rows = contaRepository.sumGastosPorCategoriasBatch(user, categoriaIds, mesAtual,
                        anoAtual);
                for (Object[] row : rows) {
                    Long catId = (Long) row[0]; // Mude de UUID para Long
                    BigDecimal soma = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                    gastosBatch.put(catId, soma);
                }
            }

            for (Orcamento orc : orcamentos) {
                Long catId = orc.getCategoria() != null ? orc.getCategoria().getId() : null;
                BigDecimal gasto = catId != null ? gastosBatch.getOrDefault(catId, BigDecimal.ZERO) : BigDecimal.ZERO;

                gastosAtuais.put(orc.getId(), gasto);

                BigDecimal limite = orc.getLimiteMensal();
                BigDecimal percentual = BigDecimal.ZERO;
                String status = "NORMAL";

                if (limite != null && limite.compareTo(BigDecimal.ZERO) > 0) {
                    percentual = gasto.multiply(new BigDecimal("100")).divide(limite, 2, RoundingMode.HALF_UP);
                    if (percentual.compareTo(new BigDecimal("100")) >= 0) {
                        status = "CRITICO";
                    } else if (percentual.compareTo(new BigDecimal("80")) >= 0) {
                        status = "ALERTA";
                    }
                }

                percentuais.put(orc.getId(), percentual);
                statusRisco.put(orc.getId(), status);
            }

            BigDecimal totalOrcado = orcamentos.stream()
                    .map(o -> o.getLimiteMensal() != null ? o.getLimiteMensal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("orcamentos", orcamentos);
            model.addAttribute("gastosAtuais", gastosAtuais);
            model.addAttribute("percentuais", percentuais);
            model.addAttribute("statusRisco", statusRisco);
            model.addAttribute("totalOrcado", totalOrcado);
        }

        String mesNome = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        mesNome = mesNome.substring(0, 1).toUpperCase() + mesNome.substring(1);

        model.addAttribute("mesAtual", mesNome);
        model.addAttribute("activeMenu", "orcamentos");
        return "orcamentos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("orcamento", new Orcamento());
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("activeMenu", "orcamentos");
        return "orcamentos/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable UUID id, Model model) {
        Orcamento orcamento = orcamentoRepository.findById(id).orElse(null);
        model.addAttribute("orcamento", orcamento);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("activeMenu", "orcamentos");
        return "orcamentos/form";
    }

    @PostMapping("/salvar")
    public String salvar(Orcamento orcamento, Principal principal, RedirectAttributes redirectAttributes) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (orcamento.getResponsavel() == null) {
            orcamento.setResponsavel(user);
        }

        if (orcamento.getCategoria() != null) {
            Orcamento existing = orcamentoRepository.findByCategoriaAndResponsavel(orcamento.getCategoria(), user)
                    .orElse(null);
            if (existing != null && !existing.getId().equals(orcamento.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Oops! Você já definiu um limite mensal para a categoria: "
                                + orcamento.getCategoria().getNome());
                return "redirect:/app/financeiro/orcamentos/novo";
            }
        }

        orcamentoRepository.save(orcamento);
        redirectAttributes.addFlashAttribute("successMessage", "Belo trabalho! Orçamento configurado com sucesso.");
        return "redirect:/app/financeiro/orcamentos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        orcamentoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Orçamento removido.");
        return "redirect:/app/financeiro/orcamentos";
    }

    @PostMapping("/gerar-automatico")
    public String gerarAutomatico(Principal principal, RedirectAttributes redirectAttributes) {
        User user = usuarioRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null)
            return "redirect:/auth/login";

        LocalDate tresMesesAtras = LocalDate.now().minusMonths(3);
        List<lucas.basemodel.modules.financeiro.models.Conta> contas = contaRepository.findAll().stream()
                .filter(c -> c.getResponsavel() != null && c.getResponsavel().getId().equals(user.getId()))
                .filter(c -> c.getTipo() == lucas.basemodel.modules.financeiro.enums.TipoTransacao.DESPESA)
                .filter(c -> c.getDataVencimento() != null && c.getDataVencimento().isAfter(tresMesesAtras))
                .toList();

        if (contas.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sem dados suficientes nos últimos 3 meses para gerar um orçamento automático.");
            return "redirect:/app/financeiro/orcamentos";
        }

        Map<Categoria, BigDecimal> gastosPorCategoria = new HashMap<>();
        for (lucas.basemodel.modules.financeiro.models.Conta c : contas) {
            if (c.getCategoria() != null) {
                gastosPorCategoria.merge(c.getCategoria(), c.getValor(), BigDecimal::add);
            }
        }

        int gerados = 0;
        for (Map.Entry<Categoria, BigDecimal> entry : gastosPorCategoria.entrySet()) {
            Categoria categoria = entry.getKey();
            BigDecimal mediaMensal = entry.getValue().divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("1.10"));

            Orcamento existing = orcamentoRepository.findByCategoriaAndResponsavel(categoria, user).orElse(null);
            if (existing == null) {
                Orcamento novo = Orcamento.builder()
                        .categoria(categoria)
                        .limiteMensal(mediaMensal)
                        .responsavel(user)
                        .build();
                orcamentoRepository.save(novo);
                gerados++;
            }
        }

        if (gerados > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mágica feita! " + gerados + " novos orçamentos gerados com base no seu padrão de gastos.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Você já possui orçamentos para todas as categorias ativas.");
        }
        return "redirect:/app/financeiro/orcamentos";
    }
}
