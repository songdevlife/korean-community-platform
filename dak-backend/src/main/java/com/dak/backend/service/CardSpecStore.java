package com.dak.backend.service;

import com.dak.backend.domain.CardSpecRecord;
import com.dak.backend.dto.CardSpec;
import com.dak.backend.repository.CardSpecRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the card specification stable.
 *
 * The model rewords its output on every call, so without this a restart
 * changed the visual description, which changed the hash identifying stored
 * artwork, which meant paying for a new illustration of a story that had not
 * changed. It also meant the card an administrator had approved was not
 * necessarily the card that would render next.
 *
 * Replaces the in-memory cache, which had the same effect within one process
 * and none at all across a restart.
 */
@Service
public class CardSpecStore {

    private static final Logger log =
            LoggerFactory.getLogger(CardSpecStore.class);

            public static final String CONTENT_AU_UPDATE = "AU_UPDATE";
            public static final String CONTENT_GUIDE = "GUIDE";

    private final CardSpecRecordRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CardSpecStore(CardSpecRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<CardSpec> find(UUID contentId) {
        return find(CONTENT_AU_UPDATE, contentId);
    }

    @Transactional(readOnly = true)
    public Optional<CardSpec> find(String contentType, UUID contentId) {

        return repository
                .findByContentTypeAndContentId(contentType, contentId)
                .flatMap(record -> {

                    try {
                        CardSpec spec = objectMapper.readValue(
                                record.getSpecJson(),
                                CardSpec.class
                        );

                        log.info(
                                "Reusing stored card spec for {}.",
                                contentId
                        );

                        return Optional.of(spec);

                    } catch (Exception e) {
                        // A spec written by an older version of the record may
                        // no longer deserialise. Regenerating is cheap and
                        // correct; failing the render is neither.
                        log.warn(
                                "Discarding unreadable card spec for {}: {}",
                                contentId,
                                e.getMessage()
                        );

                        return Optional.empty();
                    }
                });
    }

    /**
     * Runs in its own transaction: the caller renders inside a read-only one,
     * and joining it means the row is never flushed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(UUID contentId, CardSpec cardSpec) {
        save(CONTENT_AU_UPDATE, contentId, cardSpec);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String contentType, UUID contentId, CardSpec cardSpec) {

        try {
            String json = objectMapper.writeValueAsString(cardSpec);

            CardSpecRecord record = repository
                    .findByContentTypeAndContentId(contentType, contentId)
                    .orElseGet(() -> new CardSpecRecord(
                            contentType,
                            contentId,
                            json
                    ));

            record.setSpecJson(json);

            repository.save(record);

        } catch (Exception e) {
            log.warn(
                    "Could not store card spec for {}: {}",
                    contentId,
                    e.getMessage(),
                    e
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evict(UUID contentId) {
        evict(CONTENT_AU_UPDATE, contentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evict(String contentType, UUID contentId) {

        repository.deleteByContentTypeAndContentId(
                contentType,
                contentId
        );
    }
}