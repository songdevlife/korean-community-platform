package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BusinessDetailResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String phone,
        String email,
        String websiteUrl,
        String addressLine,
        String suburb,
        String state,
        String postcode,
        String country,
        Double latitude,
        Double longitude,
        String koreanAvailable,
        boolean verified,
        String status,
        List<BusinessCategoryResponse> categories,
        OffsetDateTime createdAt
) {}