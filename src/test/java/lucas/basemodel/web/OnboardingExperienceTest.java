package lucas.basemodel.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingExperienceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(username = "demo@fingen.com", roles = "USER")
    void mostraOpcoesExplicitasParaDespesasOpcionais() throws Exception {
        mockMvc.perform(get("/app/onboarding"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/index"))
                .andExpect(content().string(containsString("Não pago contas")))
                .andExpect(content().string(containsString("Prefiro não informar agora")))
                .andExpect(content().string(containsString("Sem gasto")))
                .andExpect(content().string(containsString("value=\"__NONE__\"")))
                .andExpect(content().string(containsString("transportePrincipal || null")))
                .andExpect(content().string(containsString("Quanto está disponível agora?")))
                .andExpect(content().string(containsString("Pode ser negativo")))
                .andExpect(content().string(containsString("saldoDisponivel")));
    }

    @Test
    void sessaoExpiradaNoSubmitRetornaJsonEmVezDeHtml() throws Exception {
        mockMvc.perform(post("/app/onboarding/submit")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(containsString("\"status\":\"error\"")))
                .andExpect(content().string(containsString("sessão expirou")));
    }

    @Test
    @WithMockUser(username = "demo@fingen.com", roles = "USER")
    void csrfInvalidoNoSubmitRetornaJsonEmVezDeRespostaVazia() throws Exception {
        mockMvc.perform(post("/app/onboarding/submit")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(containsString("\"status\":\"error\"")))
                .andExpect(content().string(containsString("validar a sessão")));
    }
}
