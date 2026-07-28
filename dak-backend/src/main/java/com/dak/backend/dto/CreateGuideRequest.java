package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * slug is optional. Deriving it from a Korean title produces an empty string,
 * so an admin should supply an English slug for search visibility.
 */
public record CreateGuideRequest(
        @NotBlank(message = "Enter a title.")
        @Size(max = 300, message = "Title must be 300 characters or fewer.")
        String title,

        @Size(max = 320, message = "Slug must be 320 characters or fewer.")
        String slug,

        @Size(max = 500, message = "Summary must be 500 characters or fewer.")
        String summary,

        @NotBlank(message = "Enter the guide body.")
        String body,

        UUID categoryId
) {}