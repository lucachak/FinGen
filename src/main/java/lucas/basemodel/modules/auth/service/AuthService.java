package lucas.basemodel.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.config.JwtService;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthResponse registerAndReturnToken(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .nomeCompleto(request.getUsername()) // Using username as name for simplicity or mapping accordingly
                .email(request.getEmail().toLowerCase())
                .username(request.getEmail().toLowerCase().split("@")[0])
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        User user = usuarioRepository.findByEmail(request.getEmail()).orElseThrow();
        
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
        User user = usuarioRepository.findByEmail(email).orElseThrow();
        return AuthResponse.builder()
                .token("") // Empty string instead of null for Flutter null-safety
                .userId(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .setupCompleted(user.isSetupCompleted())
                .build();
    }
}
