package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/auth/**", "/logout", "/api/v1/**")
                                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                                                .csrfTokenRepository(customCsrfTokenRepository()))
                                .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/*.glb", 
                                                                "/favicon.ico", "/favicon.svg", "/favicon-96x96.png", 
                                                                "/apple-touch-icon.png", "/site.webmanifest", "/sw.js", 
                                                                "/web-app-manifest-*.png", "/", "/auth/**")
                                                .permitAll()
                                                .requestMatchers("/app/**", "/api/**", "/api/v1/**").authenticated()
                                                .anyRequest().authenticated())
                                .sessionManagement(
                                                sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(new OncePerRequestFilter() {
                                        @Override
                                        protected void doFilterInternal(HttpServletRequest request,
                                                        HttpServletResponse response, FilterChain filterChain)
                                                        throws ServletException, IOException {
                                                CsrfToken csrfToken = (CsrfToken) request
                                                                .getAttribute(CsrfToken.class.getName());
                                                if (csrfToken != null) {
                                                        csrfToken.getToken();
                                                }
                                                filterChain.doFilter(request, response);
                                        }
                                }, CsrfFilter.class)
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                        if (request.getRequestURI().startsWith("/api/v1/")) {
                                                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                                response.setContentType("application/json");
                                                                response.getWriter().write("{\"error\": \"Unauthorized\"}");
                                                        } else {
                                                                response.sendRedirect("/auth/login");
                                                        }
                                                }))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .deleteCookies("jwtData")
                                                .invalidateHttpSession(true));

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private org.springframework.security.web.csrf.CookieCsrfTokenRepository customCsrfTokenRepository() {
                var repo = org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse();
                return repo;
        }
}