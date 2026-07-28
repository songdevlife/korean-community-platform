package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BusinessSummaryResponse(
        UUID id,
        String name,
        String slug,
        List<BusinessCategoryResponse> categories,
        String suburb,
        boolean verified,
        String koreanAvailable,
        // First image by display order, or null when the business has none.
        String thumbnailUrl,
        Double latitude,
        Double longitude,
        // Cards mark recently listed businesses; the client decides what counts
        // as recent rather than the server hard-coding a window.
        OffsetDateTime createdAt
) {}