package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "The reset link is missing its token.")
        String token,

        // Mirrors the constraint at registration. A reset that accepted a
        // weaker password than sign-up would be the easier way in.
        @NotBlank(message = "Enter a new password.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        String newPassword
) {}