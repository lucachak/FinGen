package lucas.basemodel.web;

import lucas.basemodel.modules.financeiro.services.AiModelCatalog;
import lucas.basemodel.modules.financeiro.services.AiModelInfo;
import lucas.basemodel.modules.financeiro.services.AiModelSelection;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiModelControllerTest {

    private static final String FREE = "meta-llama/llama-3.3-70b-instruct:free";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UsuarioRepository usuarioRepository;

    @MockBean
    AiModelCatalog catalog;

    private String email;

    @BeforeEach
    void setUp() {
        email = "ia-" + UUID.randomUUID() + "@example.com";
        usuarioRepository.save(User.builder()
                .email(email).username(email).password("x").nomeCompleto("Teste IA").role("USER").build());

        when(catalog.defaultModel()).thenReturn("openrouter/free");
        when(catalog.allowsPaidModels()).thenReturn(false);
        when(catalog.available()).thenReturn(List.of(
                new AiModelInfo("openrouter/free", "Free router", 0, true, false, null),
                new AiModelInfo(FREE, "Llama 3.3 70B", 65000, true, false, null)));
        when(catalog.isAllowed(anyString())).thenAnswer(i -> List.of("openrouter/free", FREE).contains(i.getArgument(0)));
        when(catalog.selectionFor(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            String model = u != null && u.getModeloIa() != null ? u.getModeloIa() : "openrouter/free";
            return new AiModelSelection(model, "google/gemma-3-27b-it:free");
        });
    }

    private User saved() {
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    @Test
    void anonymousIsNotServed() throws Exception {
        int status = mockMvc.perform(get("/api/ia/models")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(status == 302 || status == 401 || status == 403, "status " + status);
    }

    @Test
    void listsModelsWithTheCurrentSelection() throws Exception {
        mockMvc.perform(get("/api/ia/models").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected").value("openrouter/free"))
                .andExpect(jsonPath("$.allowPaid").value(false))
                .andExpect(jsonPath("$.models.length()").value(2))
                .andExpect(jsonPath("$.models[1].id").value(FREE));
    }

    @Test
    void savesAnAllowedModel() throws Exception {
        mockMvc.perform(post("/api/ia/model").with(user(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\": \"" + FREE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected").value(FREE))
                .andExpect(jsonPath("$.custom").value(true));

        assertEquals(FREE, saved().getModeloIa());
    }

    @Test
    void rejectsAModelOutsideTheCatalogueAndKeepsTheOldChoice() throws Exception {
        User user = saved();
        user.setModeloIa(FREE);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/ia/model").with(user(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\": \"openai/o1-pro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(FREE, saved().getModeloIa());
    }

    @Test
    void blankModelResetsToTheDefault() throws Exception {
        User user = saved();
        user.setModeloIa(FREE);
        usuarioRepository.save(user);

        mockMvc.perform(put("/api/v1/ia/model").with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected").value("openrouter/free"))
                .andExpect(jsonPath("$.custom").value(false));

        assertNull(saved().getModeloIa());
    }

    @Test
    void webEndpointRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/ia/model").with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\": \"" + FREE + "\"}"))
                .andExpect(status().isForbidden());

        assertNull(saved().getModeloIa());
    }
}
