package com.artha.app.api.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        Instant issuedAt
) {
    public static TokenResponse of(String access, String refresh, String username,
                                   long ttlSeconds) {
        return new TokenResponse(access, refresh, "Bearer", ttlSeconds, username, Instant.now());
    }
}