package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityPostResponse(
        UUID id,
        String category,
        String title,
        String content,
        AuthorSummary author,
        OffsetDateTime createdAt
) {
    public record AuthorSummary(UUID id, String displayName) {}
}