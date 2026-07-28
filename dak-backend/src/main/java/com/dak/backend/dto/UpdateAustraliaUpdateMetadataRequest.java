package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Sets the fields an imported draft arrives without or needs rewritten before
 * publication. All fields are nullable so an admin can change one at a time;
 * the publish check still requires category, scope and a source.
 */
public record UpdateAustraliaUpdateMetadataRequest(
        UUID categoryId,
        String geographicScope,
        @Size(max = 300) String title,
        String koreanSummary
) {}