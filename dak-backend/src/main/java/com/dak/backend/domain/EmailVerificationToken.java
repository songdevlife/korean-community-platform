package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single-use, time-limited proof that someone can read mail at an address.
 *
 * Only the hash is stored, as with reset tokens: a copy of this table should
 * not be a set of working verification links.
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken {

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

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static EmailVerificationToken createNew(User user, String tokenHash,
                                                    OffsetDateTime expiresAt) {
        EmailVerificationToken token = new EmailVerificationToken();
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