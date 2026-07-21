package com.dak.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates JWTs, per 05_API_Specification_DAK.docx §3 (access + refresh tokens).
 *
 * NOTE (tracked in 07_Backend_Development_Log.docx): refresh tokens here are self-contained JWTs,
 * not stored/revocable server-side yet. §3.4 of the API spec calls for refresh tokens to be
 * "revocable" and to support rotation — that needs a sessions table, which is deferred to a
 * later step. This is a known, intentional gap for now, not an oversight.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, ChronoUnit.MINUTES, properties.getAccessTokenExpiryMinutes());
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, ChronoUnit.DAYS, properties.getRefreshTokenExpiryDays());
    }

    private String buildToken(UUID userId, ChronoUnit unit, long amount) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(key)
                .compact();
    }

    /** Throws io.jsonwebtoken.JwtException (or a subclass) if the token is invalid/expired. */
    public UUID validateAndGetUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}