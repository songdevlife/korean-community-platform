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

    /**
     * Existing image-generation contract.
     *
     * Kept as the primary method while the card engine is being migrated,
     * so guides and existing render paths do not all need to change at once.
     */
    HeroImageResult generate(
            CardSpec.VisualSpec visual,
            CardSpec.LayoutType layoutType
    );

    /**
     * New tone-aware overload.
     *
     * During migration, implementations that do not yet use tone can continue
     * through the existing two-argument method. Once the new visual system is
     * stable, the implementations can be migrated deliberately.
     */
    default HeroImageResult generate(
            CardSpec.VisualSpec visual,
            CardSpec.LayoutType layoutType,
            CardSpec.CardTone tone
    ) {
        return generate(
                visual,
                layoutType
        );
    }

    record HeroImageResult(
            String imageUrl,
            String revisedPrompt
    ) {}
}