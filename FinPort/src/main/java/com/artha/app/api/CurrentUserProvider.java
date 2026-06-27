package com.artha.app.api;

import com.artha.app.models.User;
import com.artha.app.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently-authenticated user from Spring Security's context.
 * Phase 7 (JWT) will swap this for a token-based lookup, but the API stays the same.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        String username = auth.getName();
        User user = userRepository.findByUserName(username);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found: " + username);
        }
        return user;
    }
}