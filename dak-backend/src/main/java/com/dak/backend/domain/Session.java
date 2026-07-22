package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps to the `sessions` table (05 API Spec §3.4/§12.1).
 * Stores only the SHA-256 hash of a refresh token, never the raw value.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Session createNew(User user, String refreshTokenHash, OffsetDateTime expiresAt) {
        Session session = new Session();
        session.user = user;
        session.refreshTokenHash = refreshTokenHash;
        session.expiresAt = expiresAt;
        session.createdAt = OffsetDateTime.now();
        return session;
    }
}