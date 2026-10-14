package lucas.basemodel.web;

import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.financeiro.enums.TipoAtivo;
import lucas.basemodel.modules.financeiro.models.Investimento;
import lucas.basemodel.modules.financeiro.repositories.InvestimentoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardExperienceTest {

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired InvestimentoRepository investimentoRepository;

    @BeforeEach
    void completeSetup() {
        User user = usuarioRepository.findByEmail("lucas@admin.com").orElseThrow();
        user.setSetupCompleted(true);
        usuarioRepository.save(user);
    }

    @Test
    @WithMockUser(username = "lucas@admin.com", roles = "ADMIN")
    void rendersDecisionSummaryWithRealDataStates() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Sobra este mês")))
                .andExpect(content().string(containsString("Ocultar valores")))
                .andExpect(content().string(anyOf(
                        containsString("Seu mês ainda não tem contas"),
                        containsString("Hoje, dia "))))
                .andExpect(content().string(not(containsString("+4.2% total"))))
                .andExpect(content().string(not(containsString("R$ 450"))));
    }

    @Test
    @WithMockUser(username = "lucas@admin.com", roles = "ADMIN")
    void rendersInvestmentCockpitWithRealPositionData() throws Exception {
        User user = usuarioRepository.findByEmail("lucas@admin.com").orElseThrow();
        Investimento investimento = new Investimento();
        investimento.setNome("ETF Global");
        investimento.setTicker("WRLD11.SA");
        investimento.setTipo(TipoAtivo.ETF);
        investimento.setQuantidade(new BigDecimal("10"));
        investimento.setValorAportado(new BigDecimal("1000"));
        investimento.setValorAtual(new BigDecimal("1120"));
        investimento.setResponsavel(user);
        investimentoRepository.save(investimento);

        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"portfolio-title\"")))
                .andExpect(content().string(containsString("Abrir carteira")))
                .andExpect(content().string(containsString("ETF Global")))
                .andExpect(content().string(containsString("Referência para o próximo aporte")))
                .andExpect(content().string(containsString("Não constitui recomendação de investimento")));

        mockMvc.perform(get("/app/wealth/investimentos"))
                .andExpect(status().isOk())
                .andExpect(view().name("investimentos/lista"))
                .andExpect(content().string(containsString("Carteira de Investimentos")))
                .andExpect(content().string(containsString("Resultado Acumulado")))
                .andExpect(content().string(containsString("ETF Global")));
    }
}
