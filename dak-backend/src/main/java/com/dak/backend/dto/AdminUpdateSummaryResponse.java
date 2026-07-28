package com.dak.backend.dto;

import java.util.UUID;

public record AdminUpdateSummaryResponse(
        UUID id,
        String title,
        String status,
        boolean aiGenerated,
        boolean hasCategory,
        boolean hasSource,
        boolean hasGeographicScope,
        UUID categoryId,
        String geographicScope,
        // The reviewer's own draft, if one has been written yet.
        String koreanSummary,
        // Raw source text, for writing that draft from. Present on this
        // administrator DTO only — the public response has no equivalent field,
        // so extracted text cannot reach a reader through the API at all.
        String extractedText,
        String sourceUrl
) {}