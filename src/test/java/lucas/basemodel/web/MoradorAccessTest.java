package lucas.basemodel.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MoradorAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(username = "morador@example.com", roles = "USER")
    void moradorNaoPodeListarUsuariosDoSistema() throws Exception {
        mockMvc.perform(get("/app/settings/moradores"))
                .andExpect(status().isForbidden());
    }
}
