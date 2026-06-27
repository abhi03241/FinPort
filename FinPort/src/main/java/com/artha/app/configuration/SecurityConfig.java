package com.artha.app.configuration;

import com.artha.app.api.dto.ErrorResponse;
import com.artha.app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    /**
     * Returns a JSON 401 for unauthenticated API requests instead of redirecting
     * to the HTML login page (which would break SPA clients).
     */
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = ErrorResponse.of(401, "Unauthorized",
                    authException.getMessage(), request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationSuccessHandler customAuthenticationSuccessHandler,
            AuthenticationEntryPoint apiAuthenticationEntryPoint) throws Exception {
        http
                .authorizeHttpRequests(config -> config
                        // Public static + landing
                        .requestMatchers("/css/**", "/assets/**", "/js/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/showRegistrationForm", "/processRegistration").permitAll()
                        // OpenAPI / Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Actuator (Prometheus scraping etc.)
                        .requestMatchers("/actuator/**").permitAll()
                        // API auth (future JWT endpoint; permitted now so the SPA can hit it)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // All other /api/** require an authenticated user (session cookie for now)
                        .requestMatchers("/api/**").authenticated()
                        // Everything else
                        .anyRequest().authenticated())
                // CSRF: keep protection on server-rendered form endpoints,
                // exempt the REST API and actuator (stateless-friendly).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/**"),
                                new AntPathRequestMatcher("/actuator/**")))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")))
                .formLogin(form -> form
                        .loginPage("/showLoginPage")
                        .loginProcessingUrl("/authenticateTheUser")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll())
                .logout(logout -> logout
                        .permitAll()
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/showLoginPage")
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")));

        // Sessions are created on demand (Thymeleaf form login) but the API
        // works without one when JWT lands in Phase 7.
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}