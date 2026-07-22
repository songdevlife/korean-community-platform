package com.dak.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateAustraliaUpdateRequest(
        @NotBlank String title,
        @NotBlank String koreanSummary,
        @NotNull UUID categoryId,
        @NotBlank String geographicScope,
        @NotEmpty @Valid List<SourceInput> sources
) {
    public record SourceInput(
            @NotNull UUID sourceId,
            @NotBlank String sourceUrl,
            String sourceTitle
    ) {}
}