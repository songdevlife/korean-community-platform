package com.dak.backend.service;

import com.dak.backend.domain.CardAsset;
import com.dak.backend.dto.CardSpec;
import com.dak.backend.repository.CardAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps generated artwork where it can be found again.
 *
 * A hero costs money to produce, so it is stored against the inputs that
 * described it and reused until those inputs change. A finished card is
 * stored because publishing needs a URL rather than a byte array.
 */
@Service
public class CardAssetService {

    private static final Logger log =
            LoggerFactory.getLogger(CardAssetService.class);

    public static final String CONTENT_AU_UPDATE = "AU_UPDATE";

    private final CardAssetRepository cardAssetRepository;
    private final CloudinaryService cloudinaryService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${app.cloudinary.hero-folder:dak/heroes}")
    private String heroFolder;

    @Value("${app.cloudinary.card-folder:dak/cards}")
    private String cardFolder;

    public CardAssetService(
            CardAssetRepository cardAssetRepository,
            CloudinaryService cloudinaryService
    ) {
        this.cardAssetRepository = cardAssetRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * The stored hero for these inputs, if one exists.
     *
     * Returns the bytes rather than the URL because the renderer draws the
     * image rather than linking to it.
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> findHero(
            UUID contentId,
            CardSpec cardSpec
    ) {

        return cardAssetRepository
                .findByContentTypeAndContentIdAndAssetKindAndSpecHash(
                        CONTENT_AU_UPDATE,
                        contentId,
                        CardAsset.KIND_HERO,
                        specHash(cardSpec)
                )
                .map(asset -> {

                    log.info(
                            "Reusing stored hero for {}.",
                            contentId
                    );

                    return download(asset.getImageUrl());
                });
    }

    /**
     * Stores newly generated artwork.
     *
     * A failure here is logged and swallowed: the card has already been
     * rendered and the caller should receive it. The cost is that the next
     * render pays for the image again.
     */
    /**
     * Runs in its own transaction.
     *
     * The caller renders inside a read-only one, and joining it meant the row
     * was never flushed — the upload succeeded, no insert was issued, and
     * nothing threw. Storage is also independent of the render: a card that
     * has already been drawn should be returned whether or not its artwork
     * was successfully recorded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeHero(
            UUID contentId,
            CardSpec cardSpec,
            byte[] imageBytes
    ) {

        if (!cloudinaryService.isEnabled()) {
            return;
        }

        try {
            CloudinaryService.StoredImage stored =
                    cloudinaryService.upload(imageBytes, heroFolder);

            cardAssetRepository.save(
                    CardAsset.hero(
                            CONTENT_AU_UPDATE,
                            contentId,
                            specHash(cardSpec),
                            cardSpec.effectiveLayoutType().name(),
                            stored.imageUrl(),
                            stored.publicId()
                    )
            );

        } catch (Exception e) {
            log.warn(
                    "Could not store hero for {}: {}",
                    contentId,
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Stores a finished card, replacing whatever was stored before.
     *
     * Only one card per update is kept. Earlier versions are drafts of this
     * one rather than separate artefacts, and keeping them would mean deciding
     * later which of several images was the one actually published.
     */
    @Transactional
    public CardAsset storeCard(
            UUID contentId,
            CardSpec cardSpec,
            byte[] imageBytes
    ) {

        if (!cloudinaryService.isEnabled()) {
            throw new IllegalStateException(
                    "Cloudinary is not configured, so the card cannot be saved."
            );
        }

        List<CardAsset> existing = cardAssetRepository
                .findByContentTypeAndContentIdAndAssetKind(
                        CONTENT_AU_UPDATE,
                        contentId,
                        CardAsset.KIND_CARD
                );

        CloudinaryService.StoredImage stored =
                cloudinaryService.upload(imageBytes, cardFolder);

        CardAsset saved = cardAssetRepository.save(
                CardAsset.card(
                        CONTENT_AU_UPDATE,
                        contentId,
                        cardSpec.effectiveLayoutType().name(),
                        stored.imageUrl(),
                        stored.publicId()
                )
        );

        // After the replacement is safely stored, not before: a failed upload
        // would otherwise leave the update with no card at all.
        for (CardAsset old : existing) {
            cloudinaryService.delete(old.getPublicId());
            cardAssetRepository.delete(old);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<CardAsset> findCard(UUID contentId) {

        return cardAssetRepository
                .findByContentTypeAndContentIdAndAssetKindOrderByCreatedAtDesc(
                        CONTENT_AU_UPDATE,
                        contentId,
                        CardAsset.KIND_CARD
                )
                .stream()
                .findFirst();
    }

    /**
     * Discards stored artwork so the next render generates new artwork.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evictHeroes(UUID contentId) {

        List<CardAsset> heroes = cardAssetRepository
                .findByContentTypeAndContentIdAndAssetKind(
                        CONTENT_AU_UPDATE,
                        contentId,
                        CardAsset.KIND_HERO
                );

        for (CardAsset hero : heroes) {
            cloudinaryService.delete(hero.getPublicId());
            cardAssetRepository.delete(hero);
        }
    }

    /**
     * Identifies the inputs a hero was generated from.
     *
     * Only what reaches the image prompt counts. The Korean card text is drawn
     * by the renderer afterwards, so rewording a headline does not require new
     * artwork — but changing what the illustration depicts does.
     */
    private String specHash(CardSpec cardSpec) {

        CardSpec.VisualSpec visual = cardSpec.visual();

        StringBuilder input = new StringBuilder();

        input.append(cardSpec.effectiveLayoutType());

        if (visual != null) {
            input.append('|').append(visual.subject())
                    .append('|').append(visual.mood())
                    .append('|').append(visual.mascot())
                    .append('|').append(visual.style());
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.toString().getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not hash the visual specification.",
                    e
            );
        }
    }

    private byte[] download(String imageUrl) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Stored image returned HTTP " + response.statusCode()
                );
            }

            try (InputStream body = response.body();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                body.transferTo(buffer);

                return buffer.toByteArray();
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read the stored image.",
                    e
            );
        }
    }
}