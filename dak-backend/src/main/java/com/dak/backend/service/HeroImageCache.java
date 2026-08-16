package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary in-memory store for generated hero images.
 *
 * Without it, every preview triggers a paid image generation, which is
 * wasteful while a card layout is being adjusted repeatedly. Entries are
 * keyed by the inputs that affect the image, so editing the content or
 * changing layout naturally produces a new one.
 *
 * This is development scaffolding, not a publishing mechanism. Cached images
 * are lost on restart and are not shared between instances. Before cards are
 * published, generated artwork needs to be uploaded to Cloudinary and the
 * reference stored against the record.
 */
@Component
public class HeroImageCache {

    private static final Logger log =
            LoggerFactory.getLogger(HeroImageCache.class);

    private static final int MAX_ENTRIES = 32;

    private final Map<String, byte[]> entries =
            new LinkedHashMap<>() {

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, byte[]> eldest
                ) {
                    return size() > MAX_ENTRIES;
                }
            };

    public synchronized byte[] get(
            UUID contentId,
            CardSpec cardSpec
    ) {

        byte[] cached = entries.get(
                buildKey(contentId, cardSpec)
        );

        if (cached != null) {
            log.info(
                    "Reusing cached hero image for {}.",
                    contentId
            );
        }

        return cached;
    }

    public synchronized void put(
            UUID contentId,
            CardSpec cardSpec,
            byte[] heroImage
    ) {

        entries.put(
                buildKey(contentId, cardSpec),
                heroImage
        );
    }

    /**
     * Drops every cached image for one piece of content, whatever spec
     * produced it. Used when the caller explicitly asks for regeneration.
     */
    public synchronized void evict(UUID contentId) {

        boolean removed = entries.keySet()
                .removeIf(key -> key.startsWith(contentId + "|"));

        if (removed) {
            log.info(
                    "Evicted cached hero image for {}.",
                    contentId
            );
        }
    }

    /**
     * Only the inputs that reach the image prompt form part of the key.
     * The Korean card text is drawn separately by the renderer, so changing
     * it does not require a new illustration.
     */
    private String buildKey(
            UUID contentId,
            CardSpec cardSpec
    ) {

        CardSpec.VisualSpec visual = cardSpec.visual();

        StringBuilder key = new StringBuilder();

        key.append(contentId)
                .append('|')
                .append(cardSpec.effectiveLayoutType());

                if (visual != null) {
                        key.append('|').append(cardSpec.tone())
                                .append('|').append(visual.subject())
                                .append('|').append(visual.background())
                                .append('|').append(visual.mood())
                                .append('|').append(visual.mascot())
                                .append('|').append(visual.style());
                    }

        return key.toString();
    }
}