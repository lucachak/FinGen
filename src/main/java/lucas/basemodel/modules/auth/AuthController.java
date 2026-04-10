package lucas.basemodel.modules.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.config.JwtService;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String realizarLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            
            UserDetails user = userDetailsService.loadUserByUsername(email);
            String jwtToken = jwtService.generateToken(user);
            
            ResponseCookie jwtCookie = ResponseCookie.from("jwtData", jwtToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/")
                    .maxAge(-1) // Session-only cookie (clears on browser close)
                    .sameSite("Lax") // Protects against most CSRF attacks
                    .build();
            
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            return "redirect:/app/dashboard";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Credenciais inválidas.");
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/registar")
    public String registarPage() {
        return "auth/registar";
    }

    @PostMapping("/registar")
    public String registrarUsuario(
            @RequestParam("nomeCompleto") String nomeCompleto,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {

        if (nomeCompleto == null || nomeCompleto.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("erro", "O nome deve ter pelo menos 3 caracteres.");
            return "redirect:/auth/registar";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("erro", "A senha deve ter pelo menos 6 caracteres.");
            return "redirect:/auth/registar";
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("erro", "Este email já está em uso.");
            return "redirect:/auth/registar";
        }

        User newUser = User.builder()
                .nomeCompleto(nomeCompleto)
                .email(email.toLowerCase())
                .username(email.toLowerCase().split("@")[0])
                .password(passwordEncoder.encode(password))
                .role("USER")
                .build();

        usuarioRepository.save(newUser);
        redirectAttributes.addFlashAttribute("sucesso", "A sua jornada começa agora. Faça login para continuar.");
        return "redirect:/auth/login";
    }

    @PostMapping("/logout")
    public String logoutPost(HttpServletResponse response) {
        return logout(response);
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie.from("jwtData", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // Deletes the cookie
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return "redirect:/auth/login?logout";
    }
}
