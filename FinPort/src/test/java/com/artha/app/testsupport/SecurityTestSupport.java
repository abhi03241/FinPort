package com.artha.app.testsupport;

import com.artha.app.security.JwtService;
import com.artha.app.services.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Beans required by {@link com.artha.app.configuration.SecurityConfig} that
 * {@code @WebMvcTest} slices don't auto-load. Import this in any slice test
 * that exercises the security filter chain.
 *
 * Usage:
 * <pre>
 *   &#64;WebMvcTest(controllers = MyController.class)
 *   &#64;Import(SecurityTestSupport.class)
 *   class MyControllerTest { ... }
 * </pre>
 */
@TestConfiguration
public class SecurityTestSupport {

    @Bean
    @Primary
    public JwtService jwtService() {
        return new JwtService(
                "test-secret-test-secret-test-secret-test-secret",
                60, 14, "artha-test");
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stub UserService so the SecurityConfig's DaoAuthenticationProvider
     * bean can be constructed. Returns a minimal "user" for any username.
     */
    @Bean
    @Primary
    public UserService userService() {
        UserService svc = mock(UserService.class);
        when(svc.loadUserByUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> {
                    String username = inv.getArgument(0);
                    if ("nobody".equals(username)) {
                        throw new UsernameNotFoundException(username);
                    }
                    UserDetails u = User.withUsername(username)
                            .password("encoded")
                            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                            .build();
                    return u;
                });
        return svc;
    }
}