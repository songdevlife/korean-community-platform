package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GuideDetailResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String body,
        GuideCategoryResponse category,
        String authorName,
        String status,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}