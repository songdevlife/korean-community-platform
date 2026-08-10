package com.dak.backend.service;

import com.dak.backend.dto.CardSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary in-memory store for generated card specs.
 *
 * The model returns slightly different wording on every call, which means the
 * visual description changes too. Without a stable spec, the hero image cache
 * can never hit and every preview pays for new artwork.
 *
 * Development scaffolding, like {@link HeroImageCache}. Entries are lost on
 * restart and are not shared between instances.
 */
@Component
public class CardSpecCache {

    private static final Logger log =
            LoggerFactory.getLogger(CardSpecCache.class);

    private static final int MAX_ENTRIES = 64;

    private final Map<UUID, CardSpec> entries =
            new LinkedHashMap<>() {

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<UUID, CardSpec> eldest
                ) {
                    return size() > MAX_ENTRIES;
                }
            };

    public synchronized CardSpec get(UUID contentId) {

        CardSpec cached = entries.get(contentId);

        if (cached != null) {
            log.info(
                    "Reusing cached card spec for {}.",
                    contentId
            );
        }

        return cached;
    }

    public synchronized void put(
            UUID contentId,
            CardSpec cardSpec
    ) {
        entries.put(contentId, cardSpec);
    }

    public synchronized void evict(UUID contentId) {

        if (entries.remove(contentId) != null) {
            log.info(
                    "Evicted cached card spec for {}.",
                    contentId
            );
        }
    }
}