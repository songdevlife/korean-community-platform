package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Edits an existing business. Every field is nullable so a caller can change
 * one thing without resubmitting the whole record; omitted fields are left
 * untouched. Distinct from CreateBusinessRequest, which requires a name and
 * at least one category.
 */
public record UpdateBusinessRequest(
        @Size(max = 200) String name,
        @Size(max = 300) String shortDescription,
        String description,
        String phone,
        String email,
        String websiteUrl,
        String addressLine,
        String suburb,
        String state,
        String postcode,
        Double latitude,
        Double longitude,
        String koreanAvailable,
        Boolean verified,
        List<UUID> categoryIds
) {}