package com.dak.backend.repository;

import com.dak.backend.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = :now "
         + "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllForUser(UUID userId, OffsetDateTime now);

    @Query("SELECT COUNT(t) > 0 FROM EmailVerificationToken t "
         + "WHERE t.user.id = :userId AND t.createdAt > :since")
    boolean existsRecentForUser(UUID userId, OffsetDateTime since);
}