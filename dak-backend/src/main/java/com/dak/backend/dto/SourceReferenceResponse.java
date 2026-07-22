package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SourceReferenceResponse(
        UUID id,
        String sourceName,
        String sourceUrl,
        String sourceTitle,
        OffsetDateTime accessedAt
) {}