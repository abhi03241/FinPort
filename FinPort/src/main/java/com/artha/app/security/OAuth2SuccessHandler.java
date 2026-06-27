package com.artha.app.security;

import com.artha.app.api.dto.TokenResponse;
import com.artha.app.models.Client;
import com.artha.app.models.Role;
import com.artha.app.models.User;
import com.artha.app.repository.RoleRepository;
import com.artha.app.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Handles successful OAuth2 (Google) logins. Auto-provisions a local user on
 * first login, then issues a JWT pair and redirects the browser back to the
 * SPA with tokens in the URL fragment (e.g. /oauth/callback#access=...&refresh=...).
 *
 * Set {@code artha.oauth2.redirect-base} to override the SPA redirect target.
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String redirectBase;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                JwtService jwtService,
                                @Value("${artha.oauth2.redirect-base:http://localhost:5173/oauth/callback}") String redirectBase) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redirectBase = redirectBase;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oauthUser.getAttributes();
        String email = (String) attrs.get("email");
        String name = (String) attrs.getOrDefault("name", email);

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 user has no email attribute: {}", attrs);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth provider did not return an email");
            return;
        }

        User user = userRepository.findByUserName(email);
        if (user == null) {
            user = provisionUser(email, name);
            log.info("Provisioned new user from OAuth2 login: {}", email);
        }

        String access = jwtService.issueAccessToken(user.getUserName());
        String refresh = jwtService.issueRefreshToken(user.getUserName());
        String url = redirectBase
                + "?accessToken=" + URLEncoder.encode(access, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refresh, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private User provisionUser(String email, String displayName) {
        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole = roleRepository.save(userRole);
        }

        User u = new User();
        u.setUserName(email);
        // Random password — user logs in via OAuth, never uses it.
        u.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        u.setEnabled(true);
        Client client = new Client();
        if (displayName != null) {
            String[] parts = displayName.split(" ", 2);
            client.setFirstName(parts[0]);
            if (parts.length > 1) client.setLastName(parts[1]);
        }
        client.setEmail(email);
        u.setClient(client);
        u.setRoles(Collections.singletonList(userRole));
        return userRepository.save(u);
    }
}