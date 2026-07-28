package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Body is deliberately excluded — list endpoints should not ship full article text.
public record GuideSummaryResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        GuideCategoryResponse category,
        OffsetDateTime publishedAt
) {}