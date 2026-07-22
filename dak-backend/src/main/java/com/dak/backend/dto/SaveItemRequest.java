package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SaveItemRequest(
        @NotBlank
        @Pattern(regexp = "BUSINESS|GUIDE|COMMUNITY_POST|AUSTRALIA_UPDATE")
        String resourceType,

        @NotNull UUID resourceId
) {}