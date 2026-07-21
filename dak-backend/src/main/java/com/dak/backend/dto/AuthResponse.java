package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(UserSummary user, String accessToken, String refreshToken) {

    public record UserSummary(
            UUID id,
            String email,
            String displayName,
            String role,
            boolean emailVerified,
            OffsetDateTime createdAt
    ) {}
}