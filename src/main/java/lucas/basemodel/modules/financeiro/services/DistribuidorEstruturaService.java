package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.DistribuicaoResult;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DistribuidorEstruturaService {

    private final OrcamentoRepository orcamentoRepository;
    private final MetaFinanceiraRepository metaFinanceiraRepository;
    private final CategoriaService categoriaService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void criarEstruturaResiliente(User user, DistribuicaoResult result) {
        // 1. Remove orçamentos e metas antigos gerados pelo sistema
        orcamentoRepository.deleteByResponsavelAndIsGeradoPeloSistemaTrue(user);
        metaFinanceiraRepository.deleteByResponsavelAndIsGeradoPeloSistemaTrue(user);

        // 2. Cria novos orçamentos
        result.getOrcamentos().forEach(item -> {
            Orcamento orcamento = Orcamento.builder()
                    .categoria(categoriaService.buscarOuCriarPorNome(item.getNome()))
                    .limiteMensal(item.getValor())
                    .responsavel(user)
                    .isGeradoPeloSistema(true)
                    .build();
            orcamentoRepository.save(orcamento);
        });

        // 3. Cria novas metas financeiras
        result.getMetas().forEach(item -> {
            MetaFinanceira meta = new MetaFinanceira();
            meta.setTitulo(item.getNome());
            meta.setValorAlvo(item.getValor());
            meta.setNatureza(NaturezaMeta.RESERVA_EMERGENCIA);
            meta.setResponsavel(user);
            meta.setIsGeradoPeloSistema(true);
            metaFinanceiraRepository.save(meta);
        });
    }
}
