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

        // 1. Salvar configuração financeira
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

        // Novos campos
        config.setAjudaFamiliar(dto.getAjudaFamiliar() != null ? dto.getAjudaFamiliar() : BigDecimal.ZERO);
        config.setGastoAlimentacao(dto.getGastoAlimentacao() != null ? dto.getGastoAlimentacao() : BigDecimal.ZERO);
        config.setPossuiNegocio(dto.isPossuiNegocio());
        config.setNomeNegocio(dto.getNomeNegocio());

        configRepository.save(config);
        log.info("Configuração financeira de {} salva. Moradia: {}, Negócio: {}",
                user.getEmail(), dto.getSituacaoMoradia(), dto.isPossuiNegocio());

        // 2. Gerar Transações Recorrentes segmentadas por escopo
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

        // 3. Aplicar Estratégia de Distribuição
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

    /**
     * Gera transações recorrentes com escopo correto baseado no perfil do usuário.
     *
     * Regras:
     * - COM_OS_PAIS: sem contas da casa; ajuda familiar vai como PESSOAL
     * - Outros: contas (luz, água, etc.) vão como CASA
     * - Alimentação → sempre PESSOAL / VARIAVEL
     * - Transporte → PESSOAL (é custo individual)
     * - Assinaturas → PESSOAL
     * - Moradia (aluguel/financiamento) → PESSOAL (responsabilidade individual)
     */
    private List<TransacaoRecorrente> gerarTransacoes(User user, OnboardingPayloadDTO dto) {
        List<TransacaoRecorrente> transacoes = new ArrayList<>();

        boolean moraCom = dto.getSituacaoMoradia() == SituacaoMoradia.COM_OS_PAIS;

        // ──────────────────────────────────────────────
        // Helpers internos
        // ──────────────────────────────────────────────
        java.util.function.Function<String, TransacaoRecorrente.TransacaoRecorrenteBuilder> pessoalBuilder = (titulo) ->
                TransacaoRecorrente.builder()
                        .usuario(user)
                        .titulo(titulo)
                        .tipo(TipoTransacao.DESPESA)
                        .escopo(EscopoTransacao.PESSOAL)
                        .grupo(GrupoRecorrencia.FIXA)
                        .frequencia(Frequencia.MENSAL)
                        .diaVencimento(10)
                        .automacaoAtiva(true);

        java.util.function.Function<String, TransacaoRecorrente.TransacaoRecorrenteBuilder> casaBuilder = (titulo) ->
                TransacaoRecorrente.builder()
                        .usuario(user)
                        .titulo(titulo)
                        .tipo(TipoTransacao.DESPESA)
                        .escopo(EscopoTransacao.CASA)
                        .grupo(GrupoRecorrencia.FIXA)
                        .frequencia(Frequencia.MENSAL)
                        .diaVencimento(10)
                        .automacaoAtiva(true);

        // ──────────────────────────────────────────────
        // MORADIA — escopo PESSOAL (obrigação individual)
        // ──────────────────────────────────────────────
        if (dto.getSituacaoMoradia() != null && dto.getValorMoradia() != null
                && dto.getValorMoradia().compareTo(BigDecimal.ZERO) > 0) {
            if (dto.getSituacaoMoradia() == SituacaoMoradia.ALUGUEL) {
                transacoes.add(pessoalBuilder.apply("Aluguel").valorBase(dto.getValorMoradia()).build());
            } else if (dto.getSituacaoMoradia() == SituacaoMoradia.FINANCIAMENTO) {
                transacoes.add(pessoalBuilder.apply("Prestação Imóvel").valorBase(dto.getValorMoradia()).build());
            }
        }

        // ──────────────────────────────────────────────
        // AJUDA FAMILIAR — escopo PESSOAL (mora com os pais)
        // ──────────────────────────────────────────────
        if (moraCom && dto.getAjudaFamiliar() != null && dto.getAjudaFamiliar().compareTo(BigDecimal.ZERO) > 0) {
            transacoes.add(pessoalBuilder.apply("Ajuda Familiar")
                    .valorBase(dto.getAjudaFamiliar())
                    .build());
            log.info("Transação 'Ajuda Familiar' criada com valor R$ {}", dto.getAjudaFamiliar());
        }

        // ──────────────────────────────────────────────
        // CONTAS DA CASA — escopo CASA (só se NÃO mora com os pais)
        // ──────────────────────────────────────────────
        if (!moraCom && dto.getContasCasa() != null) {
            for (String conta : dto.getContasCasa()) {
                switch (conta.toLowerCase()) {
                    case "luz" -> transacoes.add(casaBuilder.apply("Energia Elétrica").valorBase(new BigDecimal("150.00")).build());
                    case "agua" -> transacoes.add(casaBuilder.apply("Água e Esgoto").valorBase(new BigDecimal("80.00")).build());
                    case "internet" -> transacoes.add(casaBuilder.apply("Internet / Telefonia").valorBase(new BigDecimal("120.00")).build());
                    case "gas" -> transacoes.add(casaBuilder.apply("Gás Encanado").valorBase(new BigDecimal("60.00")).build());
                    case "condominio" -> transacoes.add(casaBuilder.apply("Condomínio").valorBase(new BigDecimal("350.00")).build());
                }
            }
            log.info("{} contas residenciais (CASA) criadas.", transacoes.size());
        }

        // ──────────────────────────────────────────────
        // ALIMENTAÇÃO — escopo PESSOAL / VARIAVEL
        // ──────────────────────────────────────────────
        if (dto.getGastoAlimentacao() != null && dto.getGastoAlimentacao().compareTo(BigDecimal.ZERO) > 0) {
            transacoes.add(
                    pessoalBuilder.apply("Alimentação")
                            .grupo(GrupoRecorrencia.VARIAVEL)
                            .valorBase(dto.getGastoAlimentacao())
                            .build()
            );
        }

        // ──────────────────────────────────────────────
        // TRANSPORTE — escopo PESSOAL
        // ──────────────────────────────────────────────
        if (dto.getTransportePrincipal() != null) {
            if (dto.getTransportePrincipal() == TransportePrincipal.CARRO_MOTO) {
                transacoes.add(pessoalBuilder.apply("Combustível").grupo(GrupoRecorrencia.VARIAVEL).valorBase(new BigDecimal("400.00")).build());
                transacoes.add(pessoalBuilder.apply("Seguro Auto").valorBase(new BigDecimal("150.00")).build());
                transacoes.add(pessoalBuilder.apply("IPVA (Reserva)").valorBase(new BigDecimal("100.00")).build());
            } else if (dto.getTransportePrincipal() == TransportePrincipal.TRANSPORTE_PUBLICO) {
                transacoes.add(pessoalBuilder.apply("Transporte Público").valorBase(new BigDecimal("200.00")).build());
            }
        }

        // ──────────────────────────────────────────────
        // ASSINATURAS — escopo PESSOAL
        // ──────────────────────────────────────────────
        if (dto.getAssinaturas() != null) {
            for (String sub : dto.getAssinaturas()) {
                TransacaoRecorrente.TransacaoRecorrenteBuilder subBuilder =
                        pessoalBuilder.apply(capitalize(sub));
                switch (sub.toLowerCase()) {
                    case "netflix" -> subBuilder.valorBase(new BigDecimal("39.90"));
                    case "spotify" -> subBuilder.valorBase(new BigDecimal("21.90"));
                    case "academia" -> subBuilder.valorBase(new BigDecimal("120.00"));
                    case "amazon prime" -> subBuilder.valorBase(new BigDecimal("19.90"));
                    default -> subBuilder.valorBase(new BigDecimal("50.00"));
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
