package com.dak.backend.dto;

import java.util.UUID;

public record AdminUpdateSummaryResponse(
        UUID id, String title, String status, boolean aiGenerated,
        boolean hasCategory, boolean hasSource
) {}