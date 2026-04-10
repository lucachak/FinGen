package lucas.basemodel.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Principal principal) {
        if (principal != null) {
            return "redirect:/app/dashboard";
        }
        return "home/landing";
    }
}