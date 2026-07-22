package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateBusinessStatusRequest(
        @NotBlank
        @Pattern(regexp = "DRAFT|PENDING|PUBLISHED|REJECTED|ARCHIVED",
                 message = "status must be one of DRAFT, PENDING, PUBLISHED, REJECTED, ARCHIVED")
        String status,

        String moderationNote
) {}