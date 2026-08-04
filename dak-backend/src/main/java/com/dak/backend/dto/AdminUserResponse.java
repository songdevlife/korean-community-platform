package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A user as the admin screen sees them.
 *
 * emailVerified and createdAt are here because they are what the question
 * "has anyone signed up" actually asks — a row with an unverified address is
 * an attempt rather than an account. Both were previously answered by opening
 * psql against production, which is how the credential in Entries 12 and 36
 * came to be pasted somewhere it should not have been.
 *
 * No password hash, and none of the reset or verification tokens. The record
 * lists what it carries, which is the point of using one here.
 */
public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        String accountStatus,
        boolean emailVerified,
        OffsetDateTime createdAt
) {}