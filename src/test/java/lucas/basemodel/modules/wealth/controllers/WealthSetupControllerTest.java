package lucas.basemodel.modules.wealth.controllers;

import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.services.BudgetingService;
import lucas.basemodel.modules.wealth.services.WealthService;
import lucas.basemodel.core.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WealthSetupController.class)
class WealthSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository userRepository;

    @MockBean
    private WealthService wealthService;

    @MockBean
    private BudgetingService budgetingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void testShowStep1() throws Exception {
        mockMvc.perform(post("/wealth/setup/step1")
                .with(csrf())
                .param("assetName", "Macbook")
                .param("assetValue", "2000")
                .param("assetType", "OTHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wealth/setup/step2"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void testShowStep2() throws Exception {
        mockMvc.perform(post("/wealth/setup/step2")
                .with(csrf())
                .param("incomeSource", "Test")
                .param("incomeAmount", "0.00")
                .param("benefitAmount", "0.00")) // empty/zero skips step2 processing
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wealth/setup/step3"));
    }

    @Test
    @WithMockUser(username = "lucas.antunes@example.com")
    void testTypeMismatchException_RedirectsWithError() throws Exception {
        mockMvc.perform(post("/wealth/setup/step2")
                .with(csrf())
                .param("incomeSource", "Salary")
                .param("incomeAmount", "invalid_number")) // simulate bad input
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wealth/setup/step2?error=format"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
