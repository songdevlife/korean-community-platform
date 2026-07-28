package com.dak.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        String email,

        // Written for the person filling the form, not for a log. The default
        // Bean Validation text ("size must be between 8 and 100") names a
        // constraint rather than telling anyone what to do about it.
        @NotBlank(message = "Enter a password.")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters.")
        String password,

        @NotBlank(message = "Enter a display name.")
        @Size(max = 100, message = "Display name must be 100 characters or fewer.")
        String displayName
) {}