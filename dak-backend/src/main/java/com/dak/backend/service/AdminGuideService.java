package com.dak.backend.service;

import com.dak.backend.domain.Guide;
import com.dak.backend.domain.GuideCategory;
import com.dak.backend.domain.User;
import com.dak.backend.dto.AdminGuideSummaryResponse;
import com.dak.backend.dto.CreateGuideRequest;
import com.dak.backend.dto.GuideDetailResponse;
import com.dak.backend.dto.UpdateGuideRequest;
import com.dak.backend.dto.UpdateGuideStatusRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.GuideCategoryRepository;
import com.dak.backend.repository.GuideRepository;
import com.dak.backend.dto.CardSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminGuideService {

    // Mirrors the ck_guides_status constraint. Kept here so an invalid value is
    // rejected with a clear error rather than a database exception.
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private static final String CONTENT_TYPE = CardSpecStore.CONTENT_GUIDE;

    private final GuideRepository guideRepository;
    private final GuideCategoryRepository guideCategoryRepository;
    private final CardGenerationService cardGenerationService;
    private final HeroImageGenerationService heroImageGenerationService;
    private final CardRendererService cardRendererService;
    private final CardSpecStore cardSpecStore;
    private final CardAssetService cardAssetService;

    // Stub artwork is a placeholder rather than the illustration for this
    // guide, so it is never stored.
    @Value("${app.image.openai.enabled:true}")
    private boolean imageGenerationEnabled;

    public AdminGuideService(GuideRepository guideRepository,
                             GuideCategoryRepository guideCategoryRepository,
                             CardGenerationService cardGenerationService,
                             HeroImageGenerationService heroImageGenerationService,
                             CardRendererService cardRendererService,
                             CardSpecStore cardSpecStore,
                             CardAssetService cardAssetService) {
        this.guideRepository = guideRepository;
        this.guideCategoryRepository = guideCategoryRepository;
        this.cardGenerationService = cardGenerationService;
        this.heroImageGenerationService = heroImageGenerationService;
        this.cardRendererService = cardRendererService;
        this.cardSpecStore = cardSpecStore;
        this.cardAssetService = cardAssetService;
    }

    @Transactional(readOnly = true)
    public Page<AdminGuideSummaryResponse> listAll(String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));

        Page<Guide> guides = (status != null)
                ? guideRepository.findByStatus(status, pageable)
                : guideRepository.findAll(pageable);

        // The review queue needs to see which fields are still missing before
        // an item can be published, rather than discovering it on a failed publish.
        return guides.map(g -> new AdminGuideSummaryResponse(
                g.getId(),
                g.getTitle(),
                g.getSlug(),
                g.getStatus(),
                g.getCategory() != null,
                g.getBody() != null && !g.getBody().isBlank(),
                g.getCategory() == null ? null : g.getCategory().getId(),
                g.getPublishedAt(),
                g.getCreatedAt()));
    }

    /**
     * The author comes from the authenticated principal rather than the request
     * body, so a guide cannot be attributed to another user.
     */
    @Transactional
    public GuideDetailResponse create(CreateGuideRequest request, User author) {
        GuideCategory category = null;
        if (request.categoryId() != null) {
            category = guideCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest(
                            "INVALID_CATEGORY", "Category not found."));
        }

        String slug = resolveSlug(request.slug(), request.title(), null);

        Guide guide = Guide.createNew(
                request.title().trim(),
                slug,
                request.summary() == null ? null : request.summary().trim(),
                request.body(),
                category,
                author);

        return GuideService.toDetail(guideRepository.save(guide));
    }

    @Transactional
    public GuideDetailResponse update(UUID guideId, UpdateGuideRequest request) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> ApiException.notFound("Guide not found."));

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw ApiException.badRequest("INVALID_TITLE", "Title cannot be empty.");
            }
            guide.setTitle(title);
        }

        // Changing a published slug breaks existing links. Left as a plain update
        // for now; a redirect strategy is required before this ships publicly.
        if (request.slug() != null) {
            guide.setSlug(resolveSlug(request.slug(), guide.getTitle(), guide.getId()));
        }

        if (request.summary() != null) {
            guide.setSummary(request.summary().trim());
        }

        if (request.body() != null) {
            if (request.body().isBlank()) {
                throw ApiException.badRequest("INVALID_BODY", "Body cannot be empty.");
            }
            guide.setBody(request.body());
        }

        if (request.categoryId() != null) {
            GuideCategory category = guideCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest(
                            "INVALID_CATEGORY", "Category not found."));
            guide.setCategory(category);
        }

        guide.touch();
        return GuideService.toDetail(guide);
    }

    @Transactional
    public GuideDetailResponse updateStatus(UUID guideId, UpdateGuideStatusRequest request) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> ApiException.notFound("Guide not found."));

        String status = request.status().trim().toUpperCase();
        if (!VALID_STATUSES.contains(status)) {
            throw ApiException.badRequest("INVALID_STATUS",
                    "Status must be one of: " + String.join(", ", VALID_STATUSES));
        }

        if ("PUBLISHED".equals(status)) {
            // Same publication gate as AdminAustraliaUpdateService: block the
            // transition rather than allowing an incomplete public record.
            if (guide.getCategory() == null) {
                throw ApiException.badRequest("MISSING_CATEGORY",
                        "A Guide must have a category before it can be published.");
            }
            if (guide.getBody() == null || guide.getBody().isBlank()) {
                throw ApiException.badRequest("MISSING_BODY",
                        "A Guide must have body content before it can be published.");
            }
            guide.markPublished();
        } else {
            guide.setStatus(status);
            guide.touch();
        }

        return GuideService.toDetail(guide);
    }
// --- Cards ---

@Transactional(readOnly = true)
public CardSpec generateCardSpec(UUID guideId) {
    return generateCardSpec(guideId, false);
}

/**
 * Produces the card spec, reusing the stored one where available.
 *
 * A guide changes far less often than a news item, which makes the stored
 * spec more valuable here than it is for updates: a guide published in
 * March should still render the card an administrator approved in March.
 */
@Transactional(readOnly = true)
public CardSpec generateCardSpec(UUID guideId, boolean regenerate) {

    if (regenerate) {
        cardSpecStore.evict(CONTENT_TYPE, guideId);
    } else {
        Optional<CardSpec> stored = cardSpecStore.find(CONTENT_TYPE, guideId);

        if (stored.isPresent()) {
            return stored.get();
        }
    }

    Guide guide = guideRepository.findById(guideId)
            .orElseThrow(() -> ApiException.notFound("Guide not found."));

    if (guide.getBody() == null || guide.getBody().isBlank()) {
        throw ApiException.badRequest(
                "MISSING_BODY",
                "A Guide needs body content before a card can be generated."
        );
    }

    CardSpec cardSpec = cardGenerationService.generateForGuide(
            guide.getTitle(),
            guide.getSummary(),
            guide.getBody()
    );

    cardSpecStore.save(CONTENT_TYPE, guideId, cardSpec);

    return cardSpec;
}

/**
 * Renders one card. Index 0 is the cover; later indexes are the cards of
 * a carousel, which guides produce more often than news does — a guide is
 * usually a sequence rather than a single fact.
 */
@Transactional(readOnly = true)
public CardRendererService.RenderedCard generateCardPreview(
        UUID guideId,
        boolean regenerate,
        int cardIndex
) {

    CardSpec cardSpec = generateCardSpec(guideId, regenerate);

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
        cardAssetService.evictHeroes(CONTENT_TYPE, guideId);
    }

    byte[] heroBytes = null;

    if (needsHero) {

        heroBytes = cardAssetService
                .findHero(CONTENT_TYPE, guideId, cardSpec)
                .orElse(null);

        if (heroBytes == null) {

            HeroImageGenerationService.HeroImageResult heroResult =
                    heroImageGenerationService.generate(
                            cardSpec.visual(),
                            cardSpec.effectiveLayoutType()
                    );

            heroBytes = decodeHeroImage(heroResult.imageUrl());

            if (imageGenerationEnabled) {
                cardAssetService.storeHero(
                        CONTENT_TYPE,
                        guideId,
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

@Transactional(readOnly = true)
public int countCards(UUID guideId) {
    return 1 + generateCardSpec(guideId).usableCarouselCards().size();
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
    /**
     * Prefers an admin-supplied slug and falls back to deriving one from the
     * title. A Korean title reduces to an empty string, so the UUID fallback
     * keeps the URL valid — but the result is not search-friendly, which is why
     * the request accepts an explicit slug.
     */
    private String resolveSlug(String requestedSlug, String title, UUID currentGuideId) {
        String base = (requestedSlug != null && !requestedSlug.isBlank())
                ? slugify(requestedSlug)
                : slugify(title);

        if (base.isBlank()) {
            base = "guide-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, currentGuideId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    // On update, the guide's own slug must not count as a collision.
    private boolean slugTaken(String slug, UUID currentGuideId) {
        return currentGuideId == null
                ? guideRepository.existsBySlug(slug)
                : guideRepository.existsBySlugAndIdNot(slug, currentGuideId);
    }

    private static String slugify(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}