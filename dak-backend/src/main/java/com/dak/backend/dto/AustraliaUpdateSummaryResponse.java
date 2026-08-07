package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AustraliaUpdateSummaryResponse(
        UUID id,
        String slug,
        String title,
        // The administrator-written Korean summary. Carried in full rather than
        // truncated server-side: the list is small, and the card decides its own
        // clamp so a layout change does not need a backend one. Never contains
        // extracted source text — that field exists only on the admin DTO.
        String koreanSummary,
        UpdateCategoryResponse category,
        String geographicScope,
        boolean aiGenerated,
        OffsetDateTime createdAt
) {}