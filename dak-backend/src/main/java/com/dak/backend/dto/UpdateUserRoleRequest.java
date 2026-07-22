package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank
        @Pattern(regexp = "USER|BUSINESS_OWNER|MODERATOR|ADMINISTRATOR",
                 message = "role must be one of USER, BUSINESS_OWNER, MODERATOR, ADMINISTRATOR")
        String role
) {}