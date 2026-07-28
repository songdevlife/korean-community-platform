package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirrors AdminUpdateSummaryResponse: the review queue needs to see at a glance
 * which fields are still missing before an item can be published.
 */
public record AdminGuideSummaryResponse(
        UUID id,
        String title,
        String slug,
        String status,
        boolean hasCategory,
        boolean hasBody,
        UUID categoryId,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {}