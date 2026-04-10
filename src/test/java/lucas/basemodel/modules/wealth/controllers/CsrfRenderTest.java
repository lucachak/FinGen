package lucas.basemodel.modules.wealth.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
public class CsrfRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "lucas@admin.com")
    public void testThymeleafCsrfRendering() throws Exception {
        MvcResult result = mockMvc.perform(get("/wealth/setup/step2"))
                .andReturn();
                
        String content = result.getResponse().getContentAsString();
        System.out.println("----- HTML FOR STEP 2 -----");
        if (content.contains("name=\"_csrf\"")) {
            System.out.println("CSRF TOKEN FOUND IN HTML!");
            int index = content.indexOf("name=\"_csrf\"");
            System.out.println(content.substring(Math.max(0, index - 50), Math.min(content.length(), index + 150)));
        } else {
            System.out.println("NO CSRF TOKEN FOUND IN HTML!");
        }
    }
}
