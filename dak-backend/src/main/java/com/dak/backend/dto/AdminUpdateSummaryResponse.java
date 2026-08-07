package com.dak.backend.dto;

import java.util.UUID;

public record AdminUpdateSummaryResponse(
        UUID id,
        // Shown in the queue and editable while the item is a draft: the
        // summariser's slug is usually good and sometimes is not, and a draft
        // is the only point at which it can still be changed.
        String slug,
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