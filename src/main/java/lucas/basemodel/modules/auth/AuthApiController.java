package lucas.basemodel.modules.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.auth.dto.LoginRequest;
import lucas.basemodel.modules.auth.dto.RegisterRequest;
import lucas.basemodel.modules.auth.dto.AuthResponse;
import lucas.basemodel.modules.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for authentication.
 * Mirrors existing form-based routes at /auth/login and /auth/register.
 * Used by the Flutter mobile client.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Body: { "email": "...", "username": "...", "password": "..." }
     * Returns: JWT token + user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerAndReturnToken(request));
    }

    /**
     * POST /api/v1/auth/login
     * Body: { "email": "...", "password": "..." }
     * Returns: JWT token + user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAndReturnToken(request));
    }

    /**
     * GET /api/v1/auth/me
     * Header: Authorization: Bearer <token>
     * Returns: current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(java.security.Principal principal) {
        return ResponseEntity.ok(authService.getCurrentUserInfo(principal.getName()));
    }
}