package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.financeiro.dto.DistribuicaoResult;
import lucas.basemodel.modules.financeiro.enums.EstrategiaDistribuicao;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.models.MetaFinanceira;
import lucas.basemodel.modules.financeiro.enums.NaturezaMeta;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.MetaFinanceiraRepository;
import lucas.basemodel.modules.financeiro.repositories.OrcamentoRepository;
import lucas.basemodel.modules.financeiro.services.strategies.DistribuicaoStrategy;
import lucas.basemodel.modules.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistribuicaoOrcamentoService {

    private final List<DistribuicaoStrategy> strategies;
    private final ConfiguracaoFinanceiraRepository configRepository;
    private final DistribuidorEstruturaService distribuidorEstruturaService;

    @Transactional
    public void aplicarEstrategia(User user, BigDecimal renda, EstrategiaDistribuicao estrategia) {
        // 1. Atualiza a configuração financeira do usuário (na transação atual para evitar conflitos)
        ConfiguracaoFinanceira config = configRepository.findByUser(user)
                .orElse(ConfiguracaoFinanceira.builder().user(user).build());
        
        config.setRendaMensalEstimada(renda);
        config.setEstrategiaDistribuicao(estrategia);
        configRepository.save(config);

        // Se for MANUAL, não fazemos nada além de salvar a config
        if (estrategia == EstrategiaDistribuicao.MANUAL) {
            return;
        }

        // 2. Seleciona a estratégia correspondente
        DistribuicaoStrategy strategy = strategies.stream()
                .filter(s -> s.getEstrategia() == estrategia)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estratégia não suportada: " + estrategia));

        // 3. Calcula os novos valores
        DistribuicaoResult result = strategy.calcular(renda);

        // 4. Delega a criação da estrutura para um serviço separado (com REQUIRES_NEW real)
        distribuidorEstruturaService.criarEstruturaResiliente(user, result);
    }
}
