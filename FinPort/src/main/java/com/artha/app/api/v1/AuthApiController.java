package com.artha.app.api.v1;

import com.artha.app.api.dto.LoginRequest;
import com.artha.app.api.dto.RefreshRequest;
import com.artha.app.api.dto.TokenResponse;
import com.artha.app.models.User;
import com.artha.app.repository.UserRepository;
import com.artha.app.security.JwtService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "JWT login, refresh, current user")
public class AuthApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApiController(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange username+password for an access + refresh token pair")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByUserName(req.username());
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issue(user);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access + refresh pair")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            Claims claims = jwtService.parse(req.refreshToken());
            if (!jwtService.isRefreshToken(claims)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a refresh token");
            }
            User user = userRepository.findByUserName(claims.getSubject());
            if (user == null || !user.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found / disabled");
            }
            return issue(user);
        } catch (JwtService.InvalidTokenException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user")
    public Object me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User u = userRepository.findByUserName(authentication.getName());
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return new MeResponse(u.getUserName(),
                u.getClient() != null ? u.getClient().getEmail() : null,
                u.isEnabled());
    }

    private TokenResponse issue(User user) {
        String access = jwtService.issueAccessToken(user.getUserName());
        String refresh = jwtService.issueRefreshToken(user.getUserName());
        return TokenResponse.of(access, refresh, user.getUserName(), jwtService.accessTtl().toSeconds());
    }

    public record MeResponse(String username, String email, boolean enabled) {}
}