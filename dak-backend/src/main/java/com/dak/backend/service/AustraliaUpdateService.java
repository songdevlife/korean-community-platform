package com.dak.backend.service;

import com.dak.backend.domain.*;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.AustraliaUpdateRepository;
import com.dak.backend.repository.UpdateCategoryRepository;
import com.dak.backend.repository.UpdateSourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AustraliaUpdateService {

    private final AustraliaUpdateRepository australiaUpdateRepository;
    private final UpdateCategoryRepository updateCategoryRepository;
    private final UpdateSourceRepository updateSourceRepository;

    public AustraliaUpdateService(AustraliaUpdateRepository australiaUpdateRepository,
                                   UpdateCategoryRepository updateCategoryRepository,
                                   UpdateSourceRepository updateSourceRepository) {
        this.australiaUpdateRepository = australiaUpdateRepository;
        this.updateCategoryRepository = updateCategoryRepository;
        this.updateSourceRepository = updateSourceRepository;
    }

    @Transactional(readOnly = true)
    public Page<AustraliaUpdateSummaryResponse> search(UUID categoryId, String scope,
                                                        String keyword, Pageable pageable) {
        return australiaUpdateRepository.search("PUBLISHED", categoryId, scope, keyword, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AustraliaUpdateDetailResponse getByIdentifier(String identifier) {
        AustraliaUpdate update = resolve(identifier)
                .orElseThrow(() -> ApiException.notFound("Australia Update not found."));

        return toDetail(update);
    }

    /**
     * Accepts either form of address.
     *
     * Updates were addressed by UUID until V26 and thirty-odd of those links are
     * in the sitemap and in Google's index already, so both resolve. The
     * response carries the canonical slug and the detail page redirects to it.
     */
    private Optional<AustraliaUpdate> resolve(String identifier) {
        try {
            return australiaUpdateRepository.findById(UUID.fromString(identifier))
                    .filter(u -> "PUBLISHED".equals(u.getStatus()));
        } catch (IllegalArgumentException notAUuid) {
            return australiaUpdateRepository.findBySlugAndStatus(identifier, "PUBLISHED");
        }
    }

    @Transactional
    public AustraliaUpdateDetailResponse create(CreateAustraliaUpdateRequest request) {
        UpdateCategory category = updateCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Category not found."));

                AustraliaUpdate update = AustraliaUpdate.createNew(
                        request.title().trim(),
                        request.koreanSummary().trim(),
                        category,
                        request.geographicScope()
                );
                // No summariser on this path, so there is no English slug to work from
                // and a Korean title slugifies to nothing. The dated fallback is what
                // an administrator would get anyway, and it can be corrected from the
                // queue before publication.
                update.setSlug(resolveSlug(null, request.title(),
                        australiaUpdateRepository::existsBySlug));

        for (CreateAustraliaUpdateRequest.SourceInput sourceInput : request.sources()) {
            UpdateSource source = updateSourceRepository.findById(sourceInput.sourceId())
                    .orElseThrow(() -> ApiException.badRequest(
                            "INVALID_SOURCE", "Source not found: " + sourceInput.sourceId()));

            UpdateSourceReference reference = UpdateSourceReference.createNew(
                    update, source, sourceInput.sourceUrl(), sourceInput.sourceTitle());
            update.getSources().add(reference);
        }

        // MVP: no AI import pipeline yet, so manually-created updates start as DRAFT
        // and require a separate admin publish step (05 API Spec §10.5).
        australiaUpdateRepository.save(update);

        return toDetail(update);
    }

    private AustraliaUpdateSummaryResponse toSummary(AustraliaUpdate u) {
        UpdateCategoryResponse category = u.getCategory() == null ? null
                : new UpdateCategoryResponse(
                        u.getCategory().getId(), u.getCategory().getName(), u.getCategory().getSlug());

                        return new AustraliaUpdateSummaryResponse(
                                u.getId(), u.getSlug(), u.getTitle(), u.getKoreanSummary(), category,
                u.getGeographicScope(), u.isAiGenerated(), u.getCreatedAt());
    }

    private AustraliaUpdateDetailResponse toDetail(AustraliaUpdate u) {
        List<SourceReferenceResponse> sources = u.getSources().stream()
        .map(s -> new SourceReferenceResponse(
                s.getId(), s.getSource().getName(), s.getSourceUrl(),
                s.getSourceTitle(),
                s.getSource().getLicenceName(), s.getSource().getLicenceUrl(),
                s.getAccessedAt()))
        .toList();

        return new AustraliaUpdateDetailResponse(
                u.getId(), u.getSlug(), u.getTitle(), u.getKoreanSummary(), toCategoryResponse(u.getCategory()),
                u.getGeographicScope(), u.getStatus(), u.isAiGenerated(), sources, u.getCreatedAt()
        );
    }

    private UpdateCategoryResponse toCategoryResponse(UpdateCategory c) {
        return new UpdateCategoryResponse(c.getId(), c.getName(), c.getSlug());
    }

    /**
     * Prefers the summariser's English slug and falls back to a dated one.
     *
     * Package-private and static because both creation paths need it: this
     * service for manual entry, AdminAustraliaUpdateService for imports. Not
     * extracted to a shared helper yet — a third caller would justify one, and
     * events deliberately differ by always appending a date.
     *
     * No date suffix here, unlike events. News does not recur, so a collision
     * means two articles about the same thing rather than the same article next
     * week, and a number is the honest way to say so.
     */
    static String resolveSlug(String suggested, String title,
                              java.util.function.Predicate<String> taken) {
        String base = slugify(suggested);
        if (base.isBlank()) {
            base = slugify(title);
        }

        // Korean titles reduce to nothing, which is the usual case here.
        if (base.isBlank()) {
            base = "update-"
                    + OffsetDateTime.now().atZoneSameInstant(ZoneId.of("Australia/Adelaide"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    /** Same rules as guides and events. */
    static String slugify(String input) {
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