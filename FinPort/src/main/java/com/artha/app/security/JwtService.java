package com.artha.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates JWT access + refresh tokens. Uses HS256 with a
 * server-side secret read from {@code artha.jwt.secret}. The secret must
 * be at least 32 bytes — startup fails fast if it's too short.
 */
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final String secret;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;
    private SecretKey key;

    public JwtService(@Value("${artha.jwt.secret:}") String secret,
                      @Value("${artha.jwt.access-ttl-minutes:60}") long accessMinutes,
                      @Value("${artha.jwt.refresh-ttl-days:14}") long refreshDays,
                      @Value("${artha.jwt.issuer:artha}") String issuer) {
        this.secret = secret;
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofDays(refreshDays);
        this.issuer = issuer;
    }

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "artha.jwt.secret is required (>= 32 chars). Set it as an env var JWT_SECRET.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "artha.jwt.secret must be at least 32 bytes (got " + bytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Package-private hook for direct unit-test construction (skips @PostConstruct). */
    void initForTest() {
        init();
    }

    public String issueAccessToken(String username) {
        return issue(username, TYPE_ACCESS, accessTtl);
    }

    public String issueRefreshToken(String username) {
        return issue(username, TYPE_REFRESH, refreshTtl);
    }

    private String issue(String username, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claims(Map.of(CLAIM_TYPE, type))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Parse + verify a token. Returns the claims or throws if invalid/expired. */
    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException ex) {
            throw new InvalidTokenException("Invalid or expired token", ex);
        }
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Duration accessTtl() { return accessTtl; }
    public Duration refreshTtl() { return refreshTtl; }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String msg, Throwable cause) { super(msg, cause); }
    }
}