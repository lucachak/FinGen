package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.HistoricoPatrimonio;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.HistoricoPatrimonioRepository;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class HistoricoPatrimonioService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoPatrimonioService.class);

    private final HistoricoPatrimonioRepository historicoRepository;
    private final InvestimentoRepository investimentoRepository;
    private final UsuarioRepository usuarioRepository;

    public HistoricoPatrimonioService(HistoricoPatrimonioRepository historicoRepository, InvestimentoRepository investimentoRepository, UsuarioRepository usuarioRepository) {
        this.historicoRepository = historicoRepository;
        this.investimentoRepository = investimentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Snapshot run every 1st of the month at 00:00
    @Scheduled(cron = "0 0 0 1 * ?")
    public void registrarSnapshotMensal() {
        log.info("Iniciando registro de snapshot de patrimônio mensal para todos os usuários...");
        
        List<User> usuarios = usuarioRepository.findAll();
        LocalDate hoje = LocalDate.now();
        LocalDate dataRef = LocalDate.of(hoje.getYear(), hoje.getMonth(), 1);

        for (User user : usuarios) {
            registrarSnapshotParaUsuario(user, dataRef);
        }
        
        log.info("Snapshot mensal concluído com sucesso.");
    }
    
    public void registrarSnapshotParaUsuario(User user, LocalDate dataRef) {
        if (historicoRepository.findByUsuarioAndDataReferencia(user, dataRef).isPresent()) {
            return;
        }

        List<Investimento> investimentos = investimentoRepository.findByResponsavel(user);
        BigDecimal patrimonioTotal = investimentos.stream()
                .map(inv -> inv.getValorAtual() != null ? inv.getValorAtual() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HistoricoPatrimonio historico = HistoricoPatrimonio.builder()
                .usuario(user)
                .dataReferencia(dataRef)
                .valorTotal(patrimonioTotal)
                .build();

        historicoRepository.save(historico);
    }
    
    // Método auxiliar para uso no Mock/Testes Iniciais
    public void gerarHistoricoFicticio(User user) {
        List<HistoricoPatrimonio> existe = historicoRepository.findByUsuarioOrderByDataReferenciaAsc(user);
        if (!existe.isEmpty()) return; // Já tem histórico real ou mockado
        
        LocalDate hoje = LocalDate.now();
        BigDecimal valorBase = new BigDecimal("1000.00");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate dataRef = LocalDate.of(hoje.getYear(), hoje.getMonth(), 1).minusMonths(i);
            
            // Incrementa aleatoriamente e simula um crescimento patrimonial
            BigDecimal crescimento = BigDecimal.valueOf(Math.random() * 500);
            valorBase = valorBase.add(crescimento);
            
            HistoricoPatrimonio historico = HistoricoPatrimonio.builder()
                .usuario(user)
                .dataReferencia(dataRef)
                .valorTotal(valorBase)
                .build();
            historicoRepository.save(historico);
        }
    }
}
