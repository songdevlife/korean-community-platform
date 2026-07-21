package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Maps to the `users` table (04_Database_Design_DAK.docx §8.1.1).
 *
 * Notes:
 * - password_hash never leaves this class as-is: DTOs (in dto/) are responsible for
 *   excluding it from any API response, per §12.2 "Password ... Never be returned through an API".
 * - accountStatus values are constrained at the DB level (ck_users_account_status in V1__init.sql)
 *   to PENDING/ACTIVE/SUSPENDED/DEACTIVATED/DELETED.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public static User createNew(String email, String passwordHash, String displayName) {
        OffsetDateTime now = OffsetDateTime.now();

        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.accountStatus = "ACTIVE";
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }
}
