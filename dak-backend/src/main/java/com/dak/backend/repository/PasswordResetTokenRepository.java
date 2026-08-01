package com.dak.backend.repository;

import com.dak.backend.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Spends every outstanding token for a user.
     *
     * Called when a new one is issued and again when a password actually
     * changes. Requesting a second reset should make the first link stop
     * working — otherwise an email that reached the wrong inbox stays valid
     * for its full thirty minutes after the real owner has already acted.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now "
         + "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllForUser(UUID userId, OffsetDateTime now);

    /**
     * Whether this user has been sent a link recently.
     *
     * Counted in the database rather than loaded and filtered: the earlier
     * version read every token in the table to answer a question about one
     * user, which is fine at one account and not at ten thousand.
     */
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t "
         + "WHERE t.user.id = :userId AND t.createdAt > :since")
    boolean existsRecentForUser(UUID userId, OffsetDateTime since);
}