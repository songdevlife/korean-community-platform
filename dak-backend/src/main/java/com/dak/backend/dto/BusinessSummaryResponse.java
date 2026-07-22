package com.dak.backend.dto;

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
        Double latitude,
        Double longitude
) {}