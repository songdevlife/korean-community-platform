package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.domain.UpdateCategory;
import com.dak.backend.domain.UpdateSource;
import com.dak.backend.domain.UpdateSourceReference;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.UpdateCategoryRepository;
import com.dak.backend.repository.UpdateSourceReferenceRepository;
import com.dak.backend.repository.UpdateSourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminAustraliaUpdateService {

    // Mirrors the ck_australia_updates_scope constraint. Kept here so an invalid
    // value is rejected with a clear error rather than a database exception.
    private static final Set<String> VALID_SCOPES = Set.of(
            "ADELAIDE", "SOUTH_AUSTRALIA", "AUSTRALIA", "COUNCIL_AREA", "SUBURB");

    private final AustraliaUpdateRepository australiaUpdateRepository;
    private final UpdateCategoryRepository updateCategoryRepository;
    private final UpdateSourceRepository updateSourceRepository;
    private final UpdateSourceReferenceRepository updateSourceReferenceRepository;
    private final UrlContentFetcher urlContentFetcher;
    private final AiSummarizationService aiSummarizationService;
    private final CardGenerationService cardGenerationService;
    private final HeroImageGenerationService heroImageGenerationService;
    private final CardRendererService cardRendererService;
    private final HeroImageCache heroImageCache;
    private final CardSpecCache cardSpecCache;
    private final CardSpecStore cardSpecStore;
    private final CardAssetService cardAssetService;

    // Stub artwork is a placeholder rather than the illustration for this
    // story, so it is never stored.
    @Value("${app.image.openai.enabled:true}")
    private boolean imageGenerationEnabled;

    public AdminAustraliaUpdateService(AustraliaUpdateRepository australiaUpdateRepository,
        UpdateCategoryRepository updateCategoryRepository,
        UpdateSourceRepository updateSourceRepository,
        UpdateSourceReferenceRepository updateSourceReferenceRepository,
        UrlContentFetcher urlContentFetcher,
        AiSummarizationService aiSummarizationService,
        CardGenerationService cardGenerationService,
        HeroImageGenerationService heroImageGenerationService,
        CardRendererService cardRendererService,
        HeroImageCache heroImageCache,
        CardSpecCache cardSpecCache,
        CardAssetService cardAssetService,
        CardSpecStore cardSpecStore) {

        this.cardGenerationService = cardGenerationService;
        this.heroImageGenerationService = heroImageGenerationService;
        this.cardRendererService = cardRendererService;
        this.heroImageCache = heroImageCache;
        this.cardSpecCache = cardSpecCache;
        this.cardAssetService = cardAssetService;
        this.cardSpecStore = cardSpecStore;
        this.australiaUpdateRepository = australiaUpdateRepository;
        this.updateCategoryRepository = updateCategoryRepository;
        this.updateSourceRepository = updateSourceRepository;
        this.updateSourceReferenceRepository = updateSourceReferenceRepository;
        this.urlContentFetcher = urlContentFetcher;
        this.aiSummarizationService = aiSummarizationService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUpdateSummaryResponse> listAll(String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));

        // Drafts are worked through in the order they arrived; published items
        // are read newest first, which is when they went out rather than when
        // they were imported.
        Page<AustraliaUpdate> updates = (status != null)
                ? australiaUpdateRepository.findByStatusOrderByPublished(status, pageable)
                : australiaUpdateRepository.findAll(pageable);

        return updates.map(u -> new AdminUpdateSummaryResponse(
                u.getId(), u.getSlug(), u.getTitle(), u.getStatus(), u.isAiGenerated(),
                u.getCategory() != null, !u.getSources().isEmpty(),
                u.getGeographicScope() != null && !u.getGeographicScope().isBlank(),
                u.getCategory() == null ? null : u.getCategory().getId(),
                u.getGeographicScope(),
                // The reviewer needs their own draft, the raw source text to
                // write it from, and a link to the original.
                u.getKoreanSummary(),
                u.getExtractedText(),
                u.getSources().stream().findFirst()
                        .map(s -> s.getSourceUrl()).orElse(null),
                u.getPublishedAt()
        ));
    }

    /**
     * Fetches a source article, has it assessed and drafted, and files it as a
     * draft for review.
     *
     * A manual import is a deliberate act — an administrator chose this URL — so
     * an irrelevant verdict does not discard it. The verdict is advisory here,
     * unlike the polling path where nobody chose anything.
     *
     * The source reference is created alongside the draft. Without it an
     * imported article carries no attribution and cannot be published at all.
     */
    @Transactional
    public ImportUpdateResponse importFromUrl(ImportUpdateRequest request) {
        UpdateSource source = updateSourceRepository.findById(request.sourceId())
                .orElseThrow(() -> ApiException.badRequest(
                        "INVALID_SOURCE", "Source not found."));

        UrlContentFetcher.FetchedContent content = urlContentFetcher.fetch(request.sourceUrl());

        String sourceTitle = content.title().isBlank() ? "Untitled (review required)" : content.title();

        SummarisationResult result = aiSummarizationService.summarize(sourceTitle, content.bodyText());

        // Same rule as the polling path: the Korean headline is what readers
        // and search engines see, the source's headline stays on the reference.
        String displayTitle = (result.koreanTitle() != null && !result.koreanTitle().isBlank())
                ? result.koreanTitle()
                : sourceTitle;

        AustraliaUpdate update = AustraliaUpdate.createDraftFromImport(
                displayTitle, content.bodyText(), result.koreanDraft());
        update.setSlug(AustraliaUpdateService.resolveSlug(
                result.slug(), displayTitle, australiaUpdateRepository::existsBySlug));
        // Scope tracks the source rather than the article: an Adelaide feed
        // produces Adelaide news. An admin can still override it.
        update.setGeographicScope(source.getDefaultGeographicScope());
        australiaUpdateRepository.save(update);

        // The source's own headline, deliberately — this is the attribution,
        // not the display title.
        UpdateSourceReference reference = UpdateSourceReference.createNew(
                update, source, request.sourceUrl(), sourceTitle);
        updateSourceReferenceRepository.save(reference);
        // Both sides of the association must be set. Saving the reference alone
        // writes the row, but the parent's collection — which the publish guard
        // reads — stays empty for the life of the persistence context.
        update.getSources().add(reference);

        return new ImportUpdateResponse(
                update.getId(), request.sourceUrl(), displayTitle, content.bodyText(), update.getStatus());
    }

    /**
     * Updates the fields an admin must supply or rewrite before publication.
     * Imports arrive with no category, no scope and possibly no summary, so
     * without this there is no route to a publishable update.
     */
    @Transactional
    public AustraliaUpdateDetailResponse updateMetadata(UUID updateId,
                                                        UpdateAustraliaUpdateMetadataRequest request) {
        AustraliaUpdate update = australiaUpdateRepository.findById(updateId)
                .orElseThrow(() -> ApiException.notFound("Australia Update not found."));

        if (request.categoryId() != null) {
            UpdateCategory category = updateCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest(
                            "INVALID_CATEGORY", "Category not found."));
            update.setCategory(category);
        }

        if (request.geographicScope() != null && !request.geographicScope().isBlank()) {
            String scope = request.geographicScope().trim().toUpperCase();
            if (!VALID_SCOPES.contains(scope)) {
                throw ApiException.badRequest("INVALID_GEOGRAPHIC_SCOPE",
                        "Geographic scope must be one of: " + String.join(", ", VALID_SCOPES));
            }
            update.setGeographicScope(scope);
        }

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw ApiException.badRequest("INVALID_TITLE", "Title cannot be empty.");
            }
            update.setTitle(title);
        }

        // Editable while it is a draft and not afterwards. The summariser's slug
        // is usually good and sometimes is not, and a draft has no links to
        // break; a published one has every link already shared under it, which
        // the UUID fallback does not cover. Same rule events enforce by
        // omitting the field entirely - here the field has to exist for drafts,
        // so the guard is a status check rather than a missing parameter.
        if (request.slug() != null) {
            if ("PUBLISHED".equals(update.getStatus())) {
                throw ApiException.badRequest("SLUG_LOCKED",
                        "The address of a published update cannot be changed.");
            }
            update.setSlug(AustraliaUpdateService.resolveSlug(
                    request.slug(), update.getTitle(),
                    s -> australiaUpdateRepository.existsBySlugAndIdNot(s, updateId)));
        }

        if (request.koreanSummary() != null) {
            String summary = request.koreanSummary().trim();
            // An empty summary is accepted here and refused at publication
            // instead: a half-written draft has to be savable, and the guard
            // that matters is the one immediately before it goes public.
            update.setKoreanSummary(summary.isEmpty() ? null : summary);
        }

        return toDetail(update);
    }

    @Transactional
    public AustraliaUpdateDetailResponse updateStatus(UUID updateId, UpdateAustraliaUpdateStatusRequest request) {
        AustraliaUpdate update = australiaUpdateRepository.findById(updateId)
                .orElseThrow(() -> ApiException.notFound("Australia Update not found."));

        if ("PUBLISHED".equals(request.status())) {
            if (update.getSources().isEmpty()) {
                throw ApiException.badRequest("MISSING_SOURCE",
                        "An Australia Update must have at least one source before it can be published.");
            }
            if (update.getCategory() == null) {
                throw ApiException.badRequest("MISSING_CATEGORY",
                        "An Australia Update must have a category before it can be published.");
            }
            if (update.getGeographicScope() == null || update.getGeographicScope().isBlank()) {
                throw ApiException.badRequest("MISSING_GEOGRAPHIC_SCOPE",
                        "An Australia Update must have a geographic scope before it can be published.");
            }
            // The guard that keeps source text off the public site. An import
            // carries extracted_text and possibly an AI draft; publishing
            // without a summary an administrator has read would put unreviewed
            // content — or the publisher's own article — on DAK.
            if (update.getKoreanSummary() == null || update.getKoreanSummary().isBlank()) {
                throw ApiException.badRequest("MISSING_SUMMARY",
                        "An Australia Update needs a Korean summary written by an administrator "
                        + "before it can be published.");
            }
        }

        update.setStatus(request.status());

        return toDetail(update);
    }

    @Transactional(readOnly = true)
    public CardSpec generateCardSpec(UUID updateId) {
        return generateCardSpec(updateId, false);
    }

    /**
     * Produces the card spec, reusing the previous one where available.
     *
     * The model rewords its output on every call, so regenerating a spec
     * for an unchanged update would also change the visual description and
     * force new paid artwork. It would also mean the card an administrator
     * approved is not the card that renders next.
     */
    @Transactional(readOnly = true)
    public CardSpec generateCardSpec(
            UUID updateId,
            boolean regenerate
    ) {

        if (regenerate) {
            cardSpecCache.evict(updateId);
            cardSpecStore.evict(updateId);
        } else {
            // The store survives a restart; the map does not, and a restart is
            // what every code change costs.
            Optional<CardSpec> stored = cardSpecStore.find(updateId);

            if (stored.isPresent()) {
                return stored.get();
            }

            CardSpec cached = cardSpecCache.get(updateId);

            if (cached != null) {
                return cached;
            }
        }

        AustraliaUpdate update = australiaUpdateRepository.findById(updateId)
                .orElseThrow(() ->
                        ApiException.notFound("Australia Update not found."));

        if (update.getKoreanSummary() == null
                || update.getKoreanSummary().isBlank()) {
            throw ApiException.badRequest(
                    "MISSING_SUMMARY",
                    "An Australia Update needs a Korean summary before a card can be generated."
            );
        }

        CardSpec cardSpec =
                cardGenerationService.generateForAustraliaUpdate(
                        update.getTitle(),
                        update.getKoreanSummary()
                );

        cardSpecCache.put(updateId, cardSpec);
        cardSpecStore.save(updateId, cardSpec);

        return cardSpec;
    }

    @Transactional(readOnly = true)
    public HeroImageGenerationService.HeroImageResult generateHeroPreview(UUID updateId) {

        CardSpec cardSpec = generateCardSpec(updateId);

        if (cardSpec.visual() == null) {
            throw ApiException.badRequest(
                    "MISSING_VISUAL_SPEC",
                    "A visual specification is required before a hero image can be generated."
            );
        }

        return heroImageGenerationService.generate(
                cardSpec.visual(),
                cardSpec.effectiveLayoutType()
        );
    }

    @Transactional(readOnly = true)
    public CardRendererService.RenderedCard generateFinalCardPreview(UUID updateId) {
        return generateFinalCardPreview(updateId, false, 0);
    }

    @Transactional(readOnly = true)
    public CardRendererService.RenderedCard generateFinalCardPreview(
            UUID updateId,
            boolean regenerate
    ) {
        return generateFinalCardPreview(updateId, regenerate, 0);
    }

    /**
     * Renders one card, reusing previously generated artwork where possible.
     *
     * Index 0 is the cover; later indexes are the cards of a carousel, where
     * the content produced one. Only the cover carries artwork, so a later
     * index neither looks for a stored hero nor generates one.
     *
     * Image generation is billed per request, so a layout adjustment must not
     * silently pay for artwork that has not changed. Passing regenerate
     * discards the stored image and produces a new one.
     */
    @Transactional(readOnly = true)
    public CardRendererService.RenderedCard generateFinalCardPreview(
            UUID updateId,
            boolean regenerate,
            int cardIndex
    ) {

        CardSpec cardSpec = generateCardSpec(updateId, regenerate);

        // Cards after the cover are text, and an infographic cover draws
        // blocks rather than artwork. Generating an illustration for either
        // would be paying for an image the card never shows.
        boolean needsHero =
                cardIndex == 0
                        && cardSpec.effectiveLayoutType()
                                != CardSpec.LayoutType.INFOGRAPHIC;

        if (needsHero && cardSpec.visual() == null) {
            throw ApiException.badRequest(
                    "MISSING_VISUAL_SPEC",
                    "A visual specification is required before a card can be rendered."
            );
        }

        if (regenerate) {
            heroImageCache.evict(updateId);
            cardAssetService.evictHeroes(updateId);
        }

        byte[] heroBytes = null;

        if (needsHero) {

            // Stored artwork first, then the in-memory copy. The store
            // survives a restart; the map does not.
            heroBytes = cardAssetService
                    .findHero(updateId, cardSpec)
                    .orElseGet(() -> heroImageCache.get(updateId, cardSpec));

            if (heroBytes == null) {

                HeroImageGenerationService.HeroImageResult heroResult =
                        heroImageGenerationService.generate(
                                cardSpec.visual(),
                                cardSpec.effectiveLayoutType()
                        );

                heroBytes = decodeHeroImage(heroResult.imageUrl());

                heroImageCache.put(updateId, cardSpec, heroBytes);

                if (imageGenerationEnabled) {
                    cardAssetService.storeHero(
                            updateId,
                            cardSpec,
                            heroBytes
                    );
                }
            }
        }

        return cardRendererService.renderCarouselCard(
                cardSpec,
                heroBytes,
                cardIndex
        );
    }

    /**
     * How many cards this update produces. One unless the content warranted
     * a carousel.
     */
    @Transactional(readOnly = true)
    public int countCards(UUID updateId) {
        return 1 + generateCardSpec(updateId).usableCarouselCards().size();
    }

    /**
     * Renders the cover and stores it, replacing any earlier one.
     *
     * Separate from preview because publishing is a decision. A preview is
     * discarded; this is the image that gets used.
     */
    @Transactional
    public com.dak.backend.domain.CardAsset saveCard(UUID updateId) {

        CardSpec cardSpec = generateCardSpec(updateId);

        CardRendererService.RenderedCard rendered =
                generateFinalCardPreview(updateId, false, 0);

        return cardAssetService.storeCard(
                updateId,
                cardSpec,
                rendered.imageBytes()
        );
    }

    private byte[] decodeHeroImage(String imageUrl) {

        String prefix = "data:image/png;base64,";

        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            throw new IllegalStateException(
                    "Hero image result did not contain a PNG data URL."
            );
        }

        try {
            return java.util.Base64.getDecoder().decode(
                    imageUrl.substring(prefix.length())
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Hero image base64 data could not be decoded.",
                    e
            );
        }
    }

    private AustraliaUpdateDetailResponse toDetail(AustraliaUpdate u) {
        List<SourceReferenceResponse> sources = u.getSources().stream()
        .map(s -> new SourceReferenceResponse(
                s.getId(), s.getSource().getName(), s.getSourceUrl(),
                s.getSourceTitle(),
                s.getSource().getLicenceName(), s.getSource().getLicenceUrl(),
                s.getAccessedAt()))
        .toList();

        UpdateCategoryResponse category = u.getCategory() == null ? null : new UpdateCategoryResponse(
                u.getCategory().getId(), u.getCategory().getName(), u.getCategory().getSlug());

        // Deliberately omits extractedText: this response serves the public
        // detail endpoint as well as the admin one.
        return new AustraliaUpdateDetailResponse(
                u.getId(), u.getSlug(), u.getTitle(), u.getKoreanSummary(), category,
                u.getGeographicScope(), u.getStatus(), u.isAiGenerated(), sources, u.getCreatedAt()
        );
    }
}
