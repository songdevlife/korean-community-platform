package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateBusinessRequest(
        @NotBlank @Size(max = 200) String name,
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
        @NotEmpty List<UUID> categoryIds
) {}