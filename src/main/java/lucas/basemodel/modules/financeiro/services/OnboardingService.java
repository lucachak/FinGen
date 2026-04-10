package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.financeiro.dto.OnboardingPayloadDTO;
import lucas.basemodel.modules.financeiro.enums.*;
import lucas.basemodel.modules.financeiro.models.*;
import lucas.basemodel.modules.financeiro.repositories.*;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final ConfiguracaoFinanceiraRepository configRepository;
    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final DistribuicaoOrcamentoService distribuicaoOrcamentoService;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void processarOnboarding(User user, OnboardingPayloadDTO dto) {
        log.info("Iniciando processamento de onboarding para o usuário: {}", user.getEmail());
        
        // 1. Validar e salvar perfil do usuário
        ConfiguracaoFinanceira config = configRepository.findByUser(user)
                .orElse(ConfiguracaoFinanceira.builder().user(user).build());

        config.setRendaMensalEstimada(dto.getRendaLiquida() != null ? dto.getRendaLiquida() : BigDecimal.ZERO);
        config.setSituacaoMoradia(dto.getSituacaoMoradia());
        config.setValorMoradia(dto.getValorMoradia());
        config.setTransportePrincipal(dto.getTransportePrincipal());
        config.setPossuiDividasAtivas(dto.isPossuiDividasAtivas());
        config.setPossuiDependentes(dto.isPossuiDependentes());
        config.setNumeroDependentes(dto.getNumeroDependentes());
        config.setEstrategiaDistribuicao(EstrategiaDistribuicao.REGRA_50_30_20);
        configRepository.save(config);
        log.info("Configuração financeira e perfil de {} salvos.", user.getEmail());

        // 2. Gerar Transações Recorrentes Base
        log.info("Gerando transações recorrentes base...");
        try {
            List<TransacaoRecorrente> recorrencias = gerarTransacoes(user, dto);
            if (!recorrencias.isEmpty()) {
                transacaoRecorrenteRepository.saveAll(recorrencias);
                log.info("{} transações recorrentes geradas.", recorrencias.size());
            } else {
                log.info("Nenhuma transação recorrente gerada (perfil minimalista).");
            }
        } catch (Exception e) {
            log.error("Erro ao gerar transações recorrentes durante onboarding", e);
        }

        // 3. Aplicar Estratégia de Distribuição (Orçamentos e Metas)
        if (dto.getRendaLiquida() != null && dto.getRendaLiquida().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Aplicando estratégia de distribuição para renda: {}", dto.getRendaLiquida());
            try {
                distribuicaoOrcamentoService.aplicarEstrategia(user, dto.getRendaLiquida(), EstrategiaDistribuicao.REGRA_50_30_20);
                log.info("Estratégia 50/30/20 aplicada com sucesso.");
            } catch (Exception e) {
                log.error("Falha ao aplicar estratégia de distribuição automática. O onboarding continuará.", e);
            }
        }

        // 4. Finalizar
        user.setSetupCompleted(true);
        usuarioRepository.save(user);
        log.info("Onboarding concluído com sucesso para: {}", user.getEmail());
    }

    private List<TransacaoRecorrente> gerarTransacoes(User user, OnboardingPayloadDTO dto) {
        List<TransacaoRecorrente> transacoes = new ArrayList<>();

        // Helper para criar instância base
        java.util.function.Function<String, TransacaoRecorrente.TransacaoRecorrenteBuilder> baseBuilder = (titulo) -> 
            TransacaoRecorrente.builder()
                .usuario(user)
                .titulo(titulo)
                .tipo(TipoTransacao.DESPESA)
                .grupo(GrupoRecorrencia.FIXA)
                .frequencia(Frequencia.MENSAL)
                .diaVencimento(10) // Padrão dia 10
                .automacaoAtiva(true);

        // Moradia
        if (dto.getSituacaoMoradia() != null && dto.getValorMoradia() != null && dto.getValorMoradia().compareTo(BigDecimal.ZERO) > 0) {
            if (dto.getSituacaoMoradia() == SituacaoMoradia.ALUGUEL) {
                transacoes.add(baseBuilder.apply("Aluguel").valorBase(dto.getValorMoradia()).build());
            } else if (dto.getSituacaoMoradia() == SituacaoMoradia.FINANCIAMENTO) {
                transacoes.add(baseBuilder.apply("Prestação Imóvel").valorBase(dto.getValorMoradia()).build());
            }
        }

        // Contas da Casa
        if (dto.getContasCasa() != null) {
            for (String conta : dto.getContasCasa()) {
                switch (conta.toLowerCase()) {
                    case "luz": transacoes.add(baseBuilder.apply("Energia Elétrica").valorBase(new BigDecimal("150.00")).build()); break;
                    case "agua": transacoes.add(baseBuilder.apply("Água e Esgoto").valorBase(new BigDecimal("80.00")).build()); break;
                    case "internet": transacoes.add(baseBuilder.apply("Internet / Telefonia").valorBase(new BigDecimal("120.00")).build()); break;
                    case "gas": transacoes.add(baseBuilder.apply("Gás Encanado").valorBase(new BigDecimal("60.00")).build()); break;
                    case "condominio": transacoes.add(baseBuilder.apply("Condomínio").valorBase(new BigDecimal("350.00")).build()); break;
                }
            }
        }

        // Transporte
        if (dto.getTransportePrincipal() != null) {
            if (dto.getTransportePrincipal() == TransportePrincipal.CARRO_MOTO) {
                transacoes.add(baseBuilder.apply("Combustível").grupo(GrupoRecorrencia.VARIAVEL).valorBase(new BigDecimal("400.00")).build());
                transacoes.add(baseBuilder.apply("Seguro Auto").valorBase(new BigDecimal("150.00")).build());
                transacoes.add(baseBuilder.apply("IPVA (Reserva)").valorBase(new BigDecimal("100.00")).build());
            } else if (dto.getTransportePrincipal() == TransportePrincipal.TRANSPORTE_PUBLICO) {
                transacoes.add(baseBuilder.apply("Transporte Público").valorBase(new BigDecimal("200.00")).build());
            }
        }

        // Assinaturas
        if (dto.getAssinaturas() != null) {
            for (String sub : dto.getAssinaturas()) {
                TransacaoRecorrente.TransacaoRecorrenteBuilder subBuilder = baseBuilder.apply(capitalize(sub)).grupo(GrupoRecorrencia.FIXA);
                switch (sub.toLowerCase()) {
                    case "netflix": subBuilder.valorBase(new BigDecimal("39.90")); break;
                    case "spotify": subBuilder.valorBase(new BigDecimal("21.90")); break;
                    case "academia": subBuilder.valorBase(new BigDecimal("120.00")); break;
                    case "amazon prime": subBuilder.valorBase(new BigDecimal("19.90")); break;
                    default: subBuilder.valorBase(new BigDecimal("50.00")); break;
                }
                transacoes.add(subBuilder.build());
            }
        }

        return transacoes;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
