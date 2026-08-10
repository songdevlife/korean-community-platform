package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;

/**
 * Serves a stored hero image instead of generating one.
 *
 * Layout work means restarting the backend, and a restart empties the hero
 * cache, so every restart paid for artwork that was never the thing being
 * changed. This removes that cost entirely: the renderer receives a real
 * transparent PNG of the right shape and nothing is billed.
 *
 * Active when app.image.openai.enabled is false. The real service is the
 * default, so nothing here can reach production by accident — it would take
 * setting the flag off in the deployed environment, which would also disable
 * generation itself.
 */
@Service
@ConditionalOnProperty(
        name = "app.image.openai.enabled",
        havingValue = "false"
)
public class StubHeroImageGenerationService
        implements HeroImageGenerationService {

    private static final Logger log =
            LoggerFactory.getLogger(StubHeroImageGenerationService.class);

    private static final String ILLUSTRATION =
            "/dev-heroes/illustration.png";

    private static final String PHOTOGRAPHIC =
            "/dev-heroes/photographic.png";

    @Override
    public HeroImageResult generate(
            CardSpec.VisualSpec visual,
            CardSpec.LayoutType layoutType
    ) {

        // Mirrors the real service's split, so a stubbed card still shows the
        // treatment the layout would actually get.
        boolean photographic =
                layoutType == CardSpec.LayoutType.FACT_HOOK
                        || layoutType == CardSpec.LayoutType.URGENT;

        String resourcePath = photographic
                ? PHOTOGRAPHIC
                : ILLUSTRATION;

        log.info(
                "Image generation is disabled; serving stored hero {}.",
                resourcePath
        );

        byte[] bytes = readResource(resourcePath);

        return new HeroImageResult(
                "data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(bytes),
                "Stored development hero. No image was generated."
        );
    }

    private byte[] readResource(String resourcePath) {

        try (InputStream input =
                     getClass().getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Development hero not found: "
                                + resourcePath
                                + ". Save one from the hero-preview endpoint, "
                                + "or set app.image.openai.enabled=true."
                );
            }

            return input.readAllBytes();

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read development hero: " + resourcePath,
                    e
            );
        }
    }
}