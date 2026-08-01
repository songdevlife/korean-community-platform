package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single-use, time-limited permission to set a new password.
 *
 * Only the hash is stored. The token itself exists in the email that was sent
 * and nowhere else, so a copy of this table is not a set of working reset
 * links.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * Set when the token is spent. Kept rather than deleted so that a second
     * click on the same link can say the link has been used, which is a more
     * useful answer than saying it does not exist.
     */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static PasswordResetToken createNew(User user, String tokenHash,
                                                OffsetDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdAt = OffsetDateTime.now();
        return token;
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}