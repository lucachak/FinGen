package lucas.basemodel.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Páginas públicas: erros e mensagens que os controllers devolvem por flash precisam aparecer. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthPagesTest {

    private static final String ERRO_LOGIN = "E-mail ou senha incorretos. Confira os dados e tente de novo.";

    @Autowired MockMvc mockMvc;

    @Test
    void loginSemErroNaoMostraAlerta() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ERRO_LOGIN))));
    }

    @Test
    void loginMostraOErroEnviadoPeloControllerEMantemOEmailDigitado() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .flashAttr("erro", ERRO_LOGIN)
                        .flashAttr("emailTentado", "ana@exemplo.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ERRO_LOGIN)))
                .andExpect(content().string(containsString("value=\"ana@exemplo.com\"")));
    }

    @Test
    void loginComParametroErrorMostraMensagemPadrao() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ERRO_LOGIN)));
    }

    @Test
    void loginMostraOSucessoDoCadastro() throws Exception {
        mockMvc.perform(get("/auth/login").flashAttr("sucesso", "Conta criada. Entre com seu e-mail e senha para começar."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conta criada.")));
    }

    @Test
    void cadastroMostraOErroDeValidacao() throws Exception {
        mockMvc.perform(get("/auth/registar").flashAttr("erro", "Este e-mail já está cadastrado. Entre com ele ou use outro."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Este e-mail já está cadastrado.")));
    }

    @Test
    void paginasPublicasNaoDependemDeCdnDeCssNemDeFontes() throws Exception {
        for (String rota : new String[] {"/", "/auth/login", "/auth/registar"}) {
            mockMvc.perform(get(rota))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("cdn.tailwindcss.com"))))
                    .andExpect(content().string(not(containsString("fonts.googleapis.com"))))
                    .andExpect(content().string(containsString("/css/app")));
        }
    }
}
