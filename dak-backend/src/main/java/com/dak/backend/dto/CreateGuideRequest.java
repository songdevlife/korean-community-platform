package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * slug is optional. Deriving it from a Korean title produces an empty string,
 * so an admin should supply an English slug for search visibility.
 */
public record CreateGuideRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 320) String slug,
        @Size(max = 500) String summary,
        @NotBlank String body,
        UUID categoryId
) {}