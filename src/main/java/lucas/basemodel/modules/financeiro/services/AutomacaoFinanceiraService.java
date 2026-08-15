package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.enums.Prioridade;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.enums.GrupoRecorrencia;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.models.TransacaoRecorrente;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.financeiro.repositories.TransacaoRecorrenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AutomacaoFinanceiraService {

    private static final Logger log = LoggerFactory.getLogger(AutomacaoFinanceiraService.class);

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final ContaRepository contaRepository;
    private final GeminiService geminiService;

    public AutomacaoFinanceiraService(TransacaoRecorrenteRepository transacaoRecorrenteRepository,
                                      ContaRepository contaRepository,
                                      GeminiService geminiService) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.contaRepository = contaRepository;
        this.geminiService = geminiService;
    }

    // Cron rodando todo dia à meia noite
    @Scheduled(cron = "0 0 0 * * ?")
    public void processarContasDiarias() {
        log.info("Iniciando rotina diária de automação financeira...");
        
        // D+5 de margem de inferência para vencimento
        LocalDate dataAlvo = LocalDate.now().plusDays(5);
        int diaAlvo = dataAlvo.getDayOfMonth();

        List<TransacaoRecorrente> templates = transacaoRecorrenteRepository.findByAutomacaoAtivaTrueAndDiaVencimento(diaAlvo);

        for (TransacaoRecorrente template : templates) {
            if (template.getGrupo() == GrupoRecorrencia.FIXA) {
                processarContasFixas(template, dataAlvo);
            } else if (template.getGrupo() == GrupoRecorrencia.VARIAVEL) {
                preverContasVariaveis(template, dataAlvo);
            }
        }
        log.info("Rotina de automação financeira concluída.");
    }

    private void processarContasFixas(TransacaoRecorrente template, LocalDate dataVencimento) {
        log.info("Processando conta FIXA: {}", template.getTitulo());

        Conta novaTransacao = criarTransacaoBase(template, dataVencimento);
        novaTransacao.setValorPrevisto(template.getValorBase());
        novaTransacao.setValorRealizado(template.getValorBase());
        novaTransacao.setValor(template.getValorBase());
        novaTransacao.setStatus(StatusTransacao.PENDENTE);

        contaRepository.save(novaTransacao);
    }

    private void preverContasVariaveis(TransacaoRecorrente template, LocalDate dataVencimento) {
        log.info("Prevendo conta VARIÁVEL (IA): {}", template.getTitulo());

        List<Conta> historico = contaRepository.findTop6ByTransacaoRecorrenteAndStatusOrderByDataVencimentoDesc(template, StatusTransacao.PAGO);

        BigDecimal valorPrevisto = geminiService.preverVariacao(historico);

        Conta novaTransacao = criarTransacaoBase(template, dataVencimento);
        novaTransacao.setValorPrevisto(valorPrevisto);
        novaTransacao.setValorRealizado(null);
        novaTransacao.setValor(valorPrevisto); // Valor provisório até a baixa definitiva
        novaTransacao.setStatus(StatusTransacao.PREVISTO_IA);

        contaRepository.save(novaTransacao);
    }

    private Conta criarTransacaoBase(TransacaoRecorrente template, LocalDate dataVencimento) {
        Conta novaTransacao = new Conta();
        novaTransacao.setTransacaoRecorrente(template);
        novaTransacao.setCategoria(template.getCategoria());
        novaTransacao.setEscopo(template.getEscopo());
        novaTransacao.setResponsavel(template.getUsuario());
        novaTransacao.setDescricao(template.getTitulo());
        novaTransacao.setTipo(template.getTipo());
        novaTransacao.setFrequencia(template.getFrequencia());
        novaTransacao.setPrioridade(Prioridade.MEDIA);
        novaTransacao.setDataVencimento(dataVencimento);
        novaTransacao.setPaga(false);
        return novaTransacao;
    }
}
