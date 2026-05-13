package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Updated SecurityConfig — adds a second filter chain for /api/v1/** routes.
 *
 * STRATEGY:
 * - /api/v1/auth/** → public (no token needed)
 * - /api/v1/** → stateless JWT (Bearer token in Authorization header)
 * - /app/** → existing session-based auth (unchanged)
 * - /auth/** → existing form login (unchanged)
 *
 * Add this as a SECOND @Bean — Spring Security matches chains in order.
 * The existing chain for Thymeleaf routes stays intact.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * API filter chain — handles /api/v1/** with JWT, stateless.
     * Order = 1 means it runs BEFORE the existing MVC chain (order = 2).
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                // Only applies to /api/v1/**
                .securityMatcher("/api/v1/**")

                // No CSRF for REST APIs (tokens protect against CSRF by design)
                .csrf(csrf -> csrf.disable())

                // Stateless — no HTTP session, no cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public: login and register
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/v1/auth/login"),
                                new AntPathRequestMatcher("/api/v1/auth/register"))
                        .permitAll()
                        // Everything else under /api/v1/** requires a valid JWT
                        .anyRequest().authenticated())

                // Inject JWT filter before the standard username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

// ─── JwtAuthenticationFilter.java ────────────────────────────────────────────
// If you don't have one yet, here is the standard implementation.
// Place in: lucas/basemodel/core/security/JwtAuthenticationFilter.java
//
// @Component
// @RequiredArgsConstructor
// public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
// private final JwtService jwtService;
// private final UserDetailsService userDetailsService;
//
// @Override
// protected void doFilterInternal(HttpServletRequest request,
// HttpServletResponse response,
// FilterChain filterChain)
// throws ServletException, IOException {
//
// final String authHeader = request.getHeader("Authorization");
//
// // If no Bearer token, skip — Spring Security will reject it downstream
// if (authHeader == null || !authHeader.startsWith("Bearer ")) {
// filterChain.doFilter(request, response);
// return;
// }
//
// final String jwt = authHeader.substring(7);
// final String userEmail = jwtService.extractUsername(jwt);
//
// if (userEmail != null &&
// SecurityContextHolder.getContext().getAuthentication() == null) {
// UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
// if (jwtService.isTokenValid(jwt, userDetails)) {
// UsernamePasswordAuthenticationToken authToken =
// new UsernamePasswordAuthenticationToken(
// userDetails, null, userDetails.getAuthorities());
// authToken.setDetails(new
// WebAuthenticationDetailsSource().buildDetails(request));
// SecurityContextHolder.getContext().setAuthentication(authToken);
// }
// }
//
// filterChain.doFilter(request, response);
// }
// }