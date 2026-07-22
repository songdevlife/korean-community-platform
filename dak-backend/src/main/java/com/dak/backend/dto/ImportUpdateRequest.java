package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ImportUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "sourceUrl must start with http:// or https://")
        String sourceUrl
) {}