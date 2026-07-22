package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AustraliaUpdateSummaryResponse(
        UUID id,
        String title,
        UpdateCategoryResponse category,
        String geographicScope,
        boolean aiGenerated,
        OffsetDateTime createdAt
) {}