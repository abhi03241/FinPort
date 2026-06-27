package com.artha.app.api.v1;

import com.artha.app.ArthaApplication;
import com.artha.app.models.Client;
import com.artha.app.models.User;
import com.artha.app.repository.UserRepository;
import com.artha.app.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Uses {@code @SpringBootTest} so the full security context (including
 * OAuth2 bits) is wired exactly as in production.
 */
@SpringBootTest(classes = ArthaApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("abhi", passwordEncoder.encode("secret"));
        user.setId(1);
        user.setEnabled(true);
        Client c = new Client(); c.setEmail("abhi@example.com");
        user.setClient(c);
        // Replace whatever was seeded (the User entity is also seeded by the
        // CommandLineRunner, so we save/overwrite this user).
        userRepository.save(user);
    }

    @Test
    void login_validCredentials_returnsTokenPair() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"abhi\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("abhi"));
    }

    @Test
    void login_badPassword_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"abhi\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankFields_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void refresh_validRefreshToken_returnsNewPair() throws Exception {
        String refresh = jwtService.issueRefreshToken("abhi");

        mvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void refresh_withAccessToken_returns401() throws Exception {
        String access = jwtService.issueAccessToken("abhi");

        mvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_garbageToken_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"not-a-jwt\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        String access = jwtService.issueAccessToken("abhi");

        mvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abhi"));
    }
}