package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;

/**
 * Generates the hero illustration used by a DAK card.
 *
 * Implementations receive the visual specification created by the
 * CardGenerationService and return a generated image result.
 *
 * This service generates only the illustration.
 * It does not add card text, DAK branding, source information,
 * or render the final social/OG card.
 */
public interface HeroImageGenerationService {

    HeroImageResult generate(
        CardSpec.VisualSpec visual,
        CardSpec.LayoutType layoutType
);

    record HeroImageResult(
            String imageUrl,
            String revisedPrompt
    ) {}
}