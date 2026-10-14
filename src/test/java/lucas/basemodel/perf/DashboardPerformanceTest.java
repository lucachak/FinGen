package lucas.basemodel.perf;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mede statements SQL e tempo de GET /app/dashboard sobre 20 mil contas.
 * Não afirma limites (evita teste frágil); grava target/perf-report.txt para comparação antes/depois.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardPerformanceTest {

    private static final int CONTAS = 20_000;
    private static final int AQUECIMENTOS = 3;
    private static final int MEDICOES = 10;

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ContaRepository contas;
    @Autowired CategoriaRepository categorias;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;

    @Test
    @WithMockUser(username = "lucas@admin.com", roles = "ADMIN")
    void dashboardComVinteMilContas() throws Exception {
        User user = usuarios.findByEmail("lucas@admin.com").orElseThrow();
        user.setSetupCompleted(true);
        usuarios.save(user);
        ContaDatasetFactory.popular(contas, categorias, user, CONTAS, LocalDate.now());
        em.flush();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        for (int i = 0; i < AQUECIMENTOS; i++) {
            em.clear();
            mockMvc.perform(get("/app/dashboard")).andExpect(status().isOk());
        }

        List<Long> tempos = new ArrayList<>();
        long statements = -1;
        for (int i = 0; i < MEDICOES; i++) {
            em.clear();
            stats.clear();
            long t0 = System.nanoTime();
            mockMvc.perform(get("/app/dashboard")).andExpect(status().isOk());
            tempos.add((System.nanoTime() - t0) / 1_000_000);
            statements = stats.getPrepareStatementCount();
        }
        Collections.sort(tempos);
        long mediana = tempos.get(tempos.size() / 2);

        String relatorio = """
                contas=%d
                jdk=%s
                statements_por_requisicao=%d
                mediana_ms=%d
                tempos_ms=%s
                """.formatted(CONTAS, System.getProperty("java.version"), statements, mediana, tempos);
        System.out.println("[perf] " + relatorio.replace("\n", " | "));
        Files.writeString(Path.of("target", "perf-report.txt"), relatorio);

        assertTrue(statements > 0, "nenhum statement contado: estatísticas do Hibernate desativadas?");
    }
}
