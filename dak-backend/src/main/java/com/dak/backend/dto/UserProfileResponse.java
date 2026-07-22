package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String profileImage,
        String role,
        boolean emailVerified,
        OffsetDateTime createdAt
) {}