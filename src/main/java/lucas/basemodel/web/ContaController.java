package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.*;
import java.math.BigDecimal;
import lucas.basemodel.modules.financeiro.services.ContaService;
import lucas.basemodel.modules.financeiro.services.CategoriaService;
import lucas.basemodel.modules.wealth.services.WealthService;
import lucas.basemodel.modules.financeiro.dto.ContaStagingForm;
import lucas.basemodel.modules.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import lucas.basemodel.modules.user.User;

@Controller
@RequestMapping("/app/financeiro/contas")
@RequiredArgsConstructor
@Slf4j
public class ContaController {

    private final ContaService contaService;
    private final CategoriaService categoriaService;
    private final UsuarioRepository usuarioRepository;
    private final WealthService wealthService;


    @GetMapping("/nova")
    public String novaConta(Model model, Principal principal) {
        model.addAttribute("activeMenu", "contas"); // LIMPEZA: Correção do menu ativo

        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

        model.addAttribute("conta", new Conta());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("assets", wealthService.getUserAssets(usuarioLogado.getId()));
        model.addAttribute("tipos", TipoTransacao.values());
        model.addAttribute("prioridades", Prioridade.values());

        return "contas/form";
    }

    @GetMapping("/editar/{id}")
    public String editarConta(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("activeMenu", "contas"); // LIMPEZA: Correção do menu ativo

        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        Conta contaExistente = contaService.buscarPorId(id, usuarioLogado);

        if (contaExistente != null && contaExistente.getResponsavel() != null) {
            // LIMPEZA: Evita erro de 'Lista Imutável' (UnsupportedOperationException) no Thymeleaf
            contaExistente.setResponsaveisRateio(new ArrayList<>(List.of(contaExistente.getResponsavel())));
        }

        model.addAttribute("conta", contaExistente);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("assets", wealthService.getUserAssets(usuarioLogado.getId()));
        model.addAttribute("tipos", TipoTransacao.values());
        model.addAttribute("prioridades", Prioridade.values());

        return "contas/form";
    }

    @GetMapping({"", "/"})
    public String listarContas(Model model, Principal principal) {
        model.addAttribute("activeMenu", "contas");

        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

        List<Conta> todasPendentes = contaService.listarTodasPendentes(usuarioLogado);
        List<Conta> historico = contaService.listarHistoricoTransacoes(usuarioLogado);

        // Cálculos de Resumo Pros Max
        BigDecimal totalPendente = todasPendentes.stream()
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.time.LocalDate now = java.time.LocalDate.now();

        BigDecimal totalReceitasMes = historico.stream()
                .filter(c -> c.getTipo() == TipoTransacao.RECEITA 
                        && c.getDataPagamento() != null 
                        && c.getDataPagamento().getMonth() == now.getMonth()
                        && c.getDataPagamento().getYear() == now.getYear())
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDespesasMes = historico.stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA 
                        && c.getDataPagamento() != null 
                        && c.getDataPagamento().getMonth() == now.getMonth()
                        && c.getDataPagamento().getYear() == now.getYear())
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalPendente", totalPendente);
        model.addAttribute("totalReceitasMes", totalReceitasMes);
        model.addAttribute("totalDespesasMes", totalDespesasMes);
        model.addAttribute("saldoMes", totalReceitasMes.subtract(totalDespesasMes));

        List<Conta> contasAtrasadas = contaService.listarContasAtrasadas(usuarioLogado);
        List<Conta> contasAVencer = contaService.listarContasAVencer(usuarioLogado);
        
        List<Conta> contasUrgentes = todasPendentes.stream()
                .filter(c -> c.getPrioridade() == Prioridade.ALTA)
                .toList();

        model.addAttribute("contasAtrasadas", contasAtrasadas);
        model.addAttribute("contasAVencer", contasAVencer);
        model.addAttribute("todasPendentes", todasPendentes);
        model.addAttribute("contasUrgentes", contasUrgentes);
        model.addAttribute("historicoTransacoes", historico);

        return "contas/lista";
    }

    @PostMapping("/salvar-lote")
    public String salvarContaLote(@ModelAttribute ContaStagingForm form, Principal principal, Model model) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        int salvos = 0;
        
        if (form.getContas() != null) {
            for (ContaStagingForm.ContaStagingItem item : form.getContas()) {
                if (item.isIncluir()) {
                    Conta novaConta = new Conta();
                    novaConta.setDescricao(item.getDescricao());
                    novaConta.setValor(item.getValor());
                    novaConta.setDataVencimento(item.getDataVencimento());
                    novaConta.setDataPagamento(item.getDataVencimento());
                    novaConta.setPaga(true);
                    novaConta.setTipo(item.getTipo());
                    novaConta.setFrequencia(item.getFrequencia());
                    novaConta.setResponsavel(usuarioLogado);
                    novaConta.setEscopo(EscopoTransacao.PESSOAL);
                    novaConta.setPrioridade(Prioridade.MEDIA);
                    
                    if (item.getCategoriaId() != null) {
                        lucas.basemodel.modules.financeiro.models.Categoria cat = categoriaService.buscarPorId(item.getCategoriaId());
                        if (cat != null) novaConta.setCategoria(cat);
                    }
                    
                    contaService.salvar(novaConta);
                    salvos++;
                }
            }
        }
        
        model.addAttribute("mensagemSucesso", "Importação concluída: " + salvos + " transações foram salvas com sucesso no seu banco de dados privado!");
        return "gestor/fragmentos :: sucesso-importacao";
    }

    @PostMapping("/salvar")
    public String salvarConta(@ModelAttribute Conta conta,
                              @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
                              @RequestParam(value = "responsaveisIds", required = false) List<UUID> responsaveisIds,
                              Principal principal,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        User usuarioLogado = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        log.info("Salvando conta para o usuário: {} - Descricao: {}", usuarioLogado.getEmail(), conta.getDescricao());

        if (responsaveisIds != null && !responsaveisIds.isEmpty()) {
            List<lucas.basemodel.modules.user.User> moradores = usuarioRepository.findAllById(responsaveisIds);
            conta.setResponsaveisRateio(moradores);
            log.info("Responsáveis por rateio definidos: {}", moradores.size());
        } else if (conta.getId() == null) {
            conta.setResponsaveisRateio(new ArrayList<>(List.of(usuarioLogado)));
            log.info("Responsável padrão (logado) definido para nova conta.");
        }

        if (arquivo != null && !arquivo.isEmpty()) {
            try {
                String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
                Path pastaUploads = Paths.get("uploads");
                if (!Files.exists(pastaUploads)) Files.createDirectories(pastaUploads);
                Path caminhoFicheiro = pastaUploads.resolve(nomeArquivo);
                Files.write(caminhoFicheiro, arquivo.getBytes());
                conta.setComprovante(nomeArquivo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            contaService.salvar(conta);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erroValidacao", e.getMessage());
            return "redirect:/app/financeiro/contas/" + (conta.getId() != null ? "editar/" + conta.getId() : "nova");
        }
        return "redirect:/app/financeiro/contas";
    }

    @PostMapping("/excluir/{id}")
    public String excluirConta(@PathVariable Long id, Principal principal) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        contaService.excluir(id, usuarioLogado);
        return "redirect:/app/financeiro/contas";
    }

    @PostMapping("/pagar/{id}")
    public String pagarContaRapido(@PathVariable Long id, Principal principal,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User usuarioLogado = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        Conta conta = contaService.buscarPorId(id, usuarioLogado);
        if (conta != null && !conta.isPaga()) {
            conta.setPaga(true);
            conta.setDataPagamento(java.time.LocalDate.now());
            try {
                contaService.salvar(conta);
            } catch (IllegalStateException e) {
                redirectAttributes.addFlashAttribute("erroValidacao", e.getMessage());
                return "redirect:/app/financeiro/contas/editar/" + id;
            }
        }
        return "redirect:/app/financeiro/contas";
    }
}