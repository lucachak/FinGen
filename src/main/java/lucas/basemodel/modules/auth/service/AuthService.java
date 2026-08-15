package lucas.basemodel.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.config.JwtService;
import lucas.basemodel.core.exceptions.ConflictException;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.auth.dto.AuthResponse;
import lucas.basemodel.modules.auth.dto.LoginRequest;
import lucas.basemodel.modules.auth.dto.RegisterRequest;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse registerAndReturnToken(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();

        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("Este e-mail já está em uso");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Este nome de usuário já está em uso");
        }

        User user = User.builder()
                .nomeCompleto(username)
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        usuarioRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .setupCompleted(user.isSetupCompleted())
                .build();
    }

    public AuthResponse loginAndReturnToken(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User user = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        String token = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .setupCompleted(user.isSetupCompleted())
                .build();
    }

    public AuthResponse getCurrentUserInfo(String email) {
        User user = usuarioRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return AuthResponse.builder()
                .token("") // Empty string instead of null for Flutter null-safety
                .userId(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .setupCompleted(user.isSetupCompleted())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
