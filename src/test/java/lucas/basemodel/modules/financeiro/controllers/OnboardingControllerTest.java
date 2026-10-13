package lucas.basemodel.modules.financeiro.controllers;

import lucas.basemodel.modules.financeiro.services.OnboardingService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardingControllerTest {

    private UsuarioRepository usuarios;
    private OnboardingService onboardingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        onboardingService = mock(OnboardingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OnboardingController(usuarios, onboardingService)).build();
    }

    @Test
    void aceitaPayloadSemDespesasOpcionais() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("novo@example.com").build();
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        mockMvc.perform(post("/app/onboarding/submit")
                        .principal(() -> user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rendaLiquida": 2500,
                                  "saldoDisponivel": -150.75,
                                  "nomeConta": "Conta principal",
                                  "situacaoMoradia": "QUITADO",
                                  "possuiNegocio": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.redirect").value("/app/dashboard"));

        verify(onboardingService).processarOnboarding(eq(user), argThat(payload ->
                payload.getContasCasa().isEmpty()
                        && payload.getAssinaturas().isEmpty()
                        && payload.getTransportePrincipal() == null
                        && payload.getGastoAlimentacao() == null
                        && payload.getSaldoDisponivel().compareTo(new java.math.BigDecimal("-150.75")) == 0));
    }

    @Test
    void erroDeParsingTambemRetornaJson() throws Exception {
        mockMvc.perform(post("/app/onboarding/submit")
                        .principal(() -> "novo@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transportePrincipal\":\"VALOR_INEXISTENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(onboardingService);
    }
}
