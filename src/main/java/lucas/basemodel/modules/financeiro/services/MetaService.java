package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.financeiro.dto.AiMetaSugestaoResponse;
import lucas.basemodel.modules.financeiro.dto.MetaRequest;
import lucas.basemodel.modules.financeiro.dto.MetaResponse;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaService {

    private final MetaFinanceiraRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<MetaResponse> listarPorUsuario(String email) {
        User user = findUser(email);
        return metaRepository.findByResponsavel(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MetaResponse buscarPorId(UUID id, String email) {
        User user = findUser(email);
        MetaFinanceira meta = metaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        if (!meta.getResponsavel().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }
        return toResponse(meta);
    }

    public MetaResponse criar(MetaRequest request, String email) {
        User user = findUser(email);
        MetaFinanceira meta = MetaFinanceira.builder()
                .titulo(request.getDescricao())
                .natureza(request.getNatureza())
                .valorAlvo(request.getValorAlvo())
                .valorAtual(request.getValorAtual())
                .prazo(request.getPrazo())
                .icone(request.getIcone())
                .responsavel(user)
                .build();
        return toResponse(metaRepository.save(meta));
    }

    public MetaResponse atualizar(UUID id, MetaRequest request, String email) {
        User user = findUser(email);
        MetaFinanceira meta = metaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        
        if (!meta.getResponsavel().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }

        meta.setTitulo(request.getDescricao());
        meta.setNatureza(request.getNatureza());
        meta.setValorAlvo(request.getValorAlvo());
        meta.setValorAtual(request.getValorAtual());
        meta.setPrazo(request.getPrazo());
        meta.setIcone(request.getIcone());

        return toResponse(metaRepository.save(meta));
    }

    public void excluir(UUID id, String email) {
        User user = findUser(email);
        MetaFinanceira meta = metaRepository.findById(id).orElseThrow();
        if (meta.getResponsavel().getId().equals(user.getId())) {
            metaRepository.delete(meta);
        }
    }

    public List<AiMetaSugestaoResponse> sugerirViaAi(String email) {
        // Implementation logic for AI suggestions
        return List.of();
    }

    public MetaResponse criarDeSugestaoAi(AiMetaSugestaoResponse sugestao, String email) {
        User user = findUser(email);
        MetaFinanceira meta = MetaFinanceira.builder()
                .titulo(sugestao.getTitulo())
                .natureza(sugestao.getNatureza())
                .valorAlvo(sugestao.getValorAlvo())
                .prazo(LocalDate.now().plusMonths(sugestao.getPrazoMeses()))
                .icone(sugestao.getIcone())
                .responsavel(user)
                .isGeradoPeloSistema(true)
                .build();
        return toResponse(metaRepository.save(meta));
    }

    private MetaResponse toResponse(MetaFinanceira m) {
        double percentual = 0.0;
        if (m.getValorAlvo().compareTo(BigDecimal.ZERO) > 0) {
            percentual = m.getValorAtual().multiply(BigDecimal.valueOf(100))
                    .divide(m.getValorAlvo(), 2, RoundingMode.HALF_UP).doubleValue();
        }
        return MetaResponse.builder()
                .id(m.getId())
                .titulo(m.getTitulo())
                .natureza(m.getNatureza())
                .valorAlvo(m.getValorAlvo())
                .valorAtual(m.getValorAtual())
                .prazo(m.getPrazo())
                .status(m.getStatus())
                .icone(m.getIcone())
                .percentualConcluido(percentual)
                .geradoPeloSistema(m.getIsGeradoPeloSistema())
                .build();
    }

    private User findUser(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
