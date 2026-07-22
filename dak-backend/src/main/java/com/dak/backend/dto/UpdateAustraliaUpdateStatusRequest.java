package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAustraliaUpdateStatusRequest(
        @NotBlank
        @Pattern(regexp = "DRAFT|PENDING_REVIEW|PUBLISHED|ARCHIVED")
        String status
) {}