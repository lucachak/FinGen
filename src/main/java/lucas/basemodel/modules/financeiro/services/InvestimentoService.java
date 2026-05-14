package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.financeiro.dto.InvestimentoPortfolioResponse;
import lucas.basemodel.modules.financeiro.dto.InvestimentoRequest;
import lucas.basemodel.modules.financeiro.dto.InvestimentoResponse;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestimentoService {

    private final InvestimentoRepository investimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MercadoService mercadoService;

    public InvestimentoPortfolioResponse getPortfolio(String email) {
        User user = findUser(email);
        List<Investimento> investimentos = investimentoRepository.findByResponsavel(user);
        
        List<InvestimentoResponse> responses = investimentos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        BigDecimal totalAportado = investimentos.stream()
                .map(Investimento::getValorAportado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAtual = investimentos.stream()
                .map(Investimento::getValorAtual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal roiTotal = totalAtual.subtract(totalAportado);
        double roiPercentual = totalAportado.compareTo(BigDecimal.ZERO) > 0 ?
                roiTotal.multiply(BigDecimal.valueOf(100)).divide(totalAportado, 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        return InvestimentoPortfolioResponse.builder()
                .ativos(responses)
                .totalAportado(totalAportado)
                .totalAtual(totalAtual)
                .roiTotal(roiTotal)
                .roiPercentual(roiPercentual)
                .build();
    }

    public InvestimentoResponse criar(InvestimentoRequest request, String email) {
        User user = findUser(email);
        Investimento inv = new Investimento();
        inv.setNome(request.getNome());
        inv.setTipo(request.getTipo());
        inv.setValorAportado(request.getValorAportado());
        inv.setValorAtual(request.getValorAtual() != null ? request.getValorAtual() : request.getValorAportado());
        inv.setTicker(request.getTicker());
        inv.setResponsavel(user);
        
        return toResponse(investimentoRepository.save(inv));
    }

    public InvestimentoResponse atualizar(UUID id, InvestimentoRequest request, String email) {
        User user = findUser(email);
        Investimento inv = investimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        
        if (!inv.getResponsavel().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }

        inv.setNome(request.getNome());
        inv.setTipo(request.getTipo());
        inv.setValorAportado(request.getValorAportado());
        inv.setValorAtual(request.getValorAtual());
        inv.setTicker(request.getTicker());

        return toResponse(investimentoRepository.save(inv));
    }

    public void excluir(UUID id, String email) {
        User user = findUser(email);
        Investimento inv = investimentoRepository.findById(id).orElseThrow();
        if (inv.getResponsavel().getId().equals(user.getId())) {
            investimentoRepository.delete(inv);
        }
    }

    public void syncCotacoes(String email) {
        mercadoService.atualizarCotacoes();
    }

    private InvestimentoResponse toResponse(Investimento i) {
        return InvestimentoResponse.builder()
                .id(i.getId())
                .nome(i.getNome())
                .tipo(i.getTipo())
                .valorAportado(i.getValorAportado())
                .valorAtual(i.getValorAtual())
                .rentabilidade(i.getRentabilidade())
                .ticker(i.getTicker())
                .dataAtualizacao(i.getDataAtualizacao())
                .build();
    }

    private User findUser(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
