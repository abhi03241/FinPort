package com.artha.app.configuration;

import com.artha.app.api.dto.ErrorResponse;
import com.artha.app.security.JwtAuthenticationFilter;
import com.artha.app.security.OAuth2SuccessHandler;
import com.artha.app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    /**
     * Base security chain — form login (Thymeleaf) + JWT (API) + JSON 401 for /api/**.
     * OAuth2 bits are added by {@link OAuth2SecurityConfig} only when a
     * {@link ClientRegistrationRepository} bean is present.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationSuccessHandler customAuthenticationSuccessHandler,
            AuthenticationEntryPoint apiAuthenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .authorizeHttpRequests(config -> config
                        .requestMatchers("/css/**", "/assets/**", "/js/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/showRegistrationForm", "/processRegistration").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/me").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/**"),
                                new AntPathRequestMatcher("/actuator/**")))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")))
                .httpBasic(b -> b.disable())
                .formLogin(form -> form
                        .loginPage("/showLoginPage")
                        .loginProcessingUrl("/authenticateTheUser")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .permitAll()
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/showLoginPage")
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")));

        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}