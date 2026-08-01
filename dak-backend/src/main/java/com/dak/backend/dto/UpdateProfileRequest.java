package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service profile edit. Only the display name is changeable: the email is
 * the login identifier and changing it needs verification that does not exist
 * yet, and the role is an administrator's decision rather than the holder's.
 *
 * Constraints mirror RegisterRequest so a name accepted at sign-up is not
 * rejected on edit.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Enter a display name.")
        @Size(max = 100, message = "Display name must be 100 characters or fewer.")
        String displayName
) {}