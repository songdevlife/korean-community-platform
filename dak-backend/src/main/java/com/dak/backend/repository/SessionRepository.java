package com.dak.backend.repository;

import com.dak.backend.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    /**
     * Revokes every live session a user holds.
     *
     * Used after a password reset. The usual reason to reset is suspecting
     * someone else has the old password, and leaving their sessions alive
     * would make the reset ceremonial — the refresh token they already hold
     * would keep issuing access tokens for another fortnight.
     */
    @Modifying
    @Query("UPDATE Session s SET s.revoked = true "
         + "WHERE s.user.id = :userId AND s.revoked = false")
    void revokeAllForUser(UUID userId);
}