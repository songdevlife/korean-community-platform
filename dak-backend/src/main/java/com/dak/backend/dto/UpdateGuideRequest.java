package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

// All fields optional — PATCH semantics, only supplied fields are changed.
public record UpdateGuideRequest(
        @Size(max = 300) String title,
        @Size(max = 320) String slug,
        @Size(max = 500) String summary,
        String body,
        UUID categoryId
) {}