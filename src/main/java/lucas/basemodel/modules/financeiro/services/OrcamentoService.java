package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.financeiro.dto.OrcamentoRequest;
import lucas.basemodel.modules.financeiro.dto.OrcamentoResponse;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public List<OrcamentoResponse> listarComConsumo(String email, EscopoTransacao escopo) {
        User user = findUser(email);
        espacoFinanceiroService.validarAcesso(user, escopo);
        List<Orcamento> orcamentos = orcamentoRepository.findByResponsavel(user).stream()
                .filter(o -> o.getCategoria() != null && o.getCategoria().getEscopo() == escopo)
                .toList();
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();

        return orcamentos.stream().map(orc -> {
            BigDecimal gasto = contaRepository.sumGastosPorCategoriaMesAno(user, orc.getCategoria(), mes, ano);
            if (gasto == null) gasto = BigDecimal.ZERO;
            BigDecimal limite = orc.getLimiteMensal() != null ? orc.getLimiteMensal() : BigDecimal.ZERO;
            double percentual = limite.compareTo(BigDecimal.ZERO) > 0
                    ? gasto.multiply(BigDecimal.valueOf(100)).divide(limite, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            return OrcamentoResponse.builder()
                    .id(orc.getId())
                    .categoriaId(orc.getCategoria() != null ? orc.getCategoria().getId() : null)
                    .categoriaNome(orc.getCategoria() != null ? orc.getCategoria().getNome() : null)
                    .limiteMensal(limite)
                    .gastoAtual(gasto)
                    .percentualConsumo(percentual)
                    .geradoPeloSistema(Boolean.TRUE.equals(orc.getIsGeradoPeloSistema()))
                    .build();
        }).collect(Collectors.toList());
    }

    public OrcamentoResponse criar(OrcamentoRequest request, String email) {
        User user = findUser(email);
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        espacoFinanceiroService.validarAcesso(user, categoria.getEscopo());
        categoriaService.validarNoEscopo(categoria, categoria.getEscopo());
        Orcamento orc = Orcamento.builder()
                .categoria(categoria)
                .limiteMensal(request.getLimiteMensal())
                .responsavel(user)
                .build();
        return toResponse(orcamentoRepository.save(orc), BigDecimal.ZERO, 0.0);
    }

    public OrcamentoResponse atualizar(UUID id, OrcamentoRequest request, String email) {
        User user = findUser(email);
        Orcamento orc = orcamentoRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        espacoFinanceiroService.validarAcesso(user, categoria.getEscopo());
        categoriaService.validarNoEscopo(categoria, categoria.getEscopo());
        orc.setCategoria(categoria);
        orc.setLimiteMensal(request.getLimiteMensal());
        return toResponse(orcamentoRepository.save(orc), BigDecimal.ZERO, 0.0);
    }

    public void excluir(UUID id, String email) {
        User user = findUser(email);
        Orcamento orcamento = orcamentoRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
        orcamentoRepository.delete(orcamento);
    }

    public List<OrcamentoResponse> gerarAutomatico(String email, EscopoTransacao escopo) {
        User user = findUser(email);
        espacoFinanceiroService.validarAcesso(user, escopo);
        LocalDate tresMesesAtras = LocalDate.now().minusMonths(3);
        List<Conta> contas = contaRepository.findByResponsavelAndPeriodo(user, tresMesesAtras, LocalDate.now()).stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .filter(c -> c.getEscopo() == escopo)
                .filter(c -> c.getDataVencimento() != null && c.getDataVencimento().isAfter(tresMesesAtras))
                .collect(Collectors.toList());

        Map<Categoria, BigDecimal> gastosPorCategoria = new HashMap<>();
        for (Conta c : contas) {
            if (c.getCategoria() != null) {
                gastosPorCategoria.merge(c.getCategoria(), c.getValor(), BigDecimal::add);
            }
        }

        List<OrcamentoResponse> gerados = new ArrayList<>();
        for (Map.Entry<Categoria, BigDecimal> entry : gastosPorCategoria.entrySet()) {
            Categoria categoria = entry.getKey();
            BigDecimal mediaMensal = entry.getValue()
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("1.10"));
            Orcamento existing = orcamentoRepository.findByCategoriaAndResponsavel(categoria, user).orElse(null);
            if (existing == null) {
                Orcamento novo = Orcamento.builder()
                        .categoria(categoria)
                        .limiteMensal(mediaMensal)
                        .responsavel(user)
                        .isGeradoPeloSistema(true)
                        .build();
                gerados.add(toResponse(orcamentoRepository.save(novo), BigDecimal.ZERO, 0.0));
            }
        }
        return gerados;
    }

    private OrcamentoResponse toResponse(Orcamento orc, BigDecimal gastoAtual, double percentual) {
        return OrcamentoResponse.builder()
                .id(orc.getId())
                .categoriaId(orc.getCategoria() != null ? orc.getCategoria().getId() : null)
                .categoriaNome(orc.getCategoria() != null ? orc.getCategoria().getNome() : null)
                .limiteMensal(orc.getLimiteMensal())
                .gastoAtual(gastoAtual)
                .percentualConsumo(percentual)
                .geradoPeloSistema(Boolean.TRUE.equals(orc.getIsGeradoPeloSistema()))
                .build();
    }

    private User findUser(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
