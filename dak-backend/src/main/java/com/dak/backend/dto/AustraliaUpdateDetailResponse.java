package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AustraliaUpdateDetailResponse(
        UUID id,
        String title,
        String koreanSummary,
        UpdateCategoryResponse category,
        String geographicScope,
        String status,
        boolean aiGenerated,
        List<SourceReferenceResponse> sources,
        OffsetDateTime createdAt
) {}