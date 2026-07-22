package com.dak.backend.dto;

import java.util.UUID;

public record ImportUpdateResponse(
        UUID draftId,
        String sourceUrl,
        String detectedTitle,
        String generatedSummary,
        String status
) {}