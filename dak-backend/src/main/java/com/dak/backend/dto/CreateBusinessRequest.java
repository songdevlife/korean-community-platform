package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateBusinessRequest(
        @NotBlank(message = "Enter the business name.")
        @Size(max = 200, message = "Business name must be 200 characters or fewer.")
        String name,

        @Size(max = 300, message = "Short description must be 300 characters or fewer.")
        String shortDescription,
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
        @NotEmpty(message = "Choose at least one category.")
        List<UUID> categoryIds,
        // Optional. MVP accepts external URLs rather than uploads; see V9 migration.
        @Size(max = 10, message = "You can add up to 10 images.")
        List<String> imageUrls
) {}