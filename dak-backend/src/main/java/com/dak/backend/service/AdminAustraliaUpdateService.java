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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public AdminAustraliaUpdateService(AustraliaUpdateRepository australiaUpdateRepository,
                                        UpdateCategoryRepository updateCategoryRepository,
                                        UpdateSourceRepository updateSourceRepository,
                                        UpdateSourceReferenceRepository updateSourceReferenceRepository,
                                        UrlContentFetcher urlContentFetcher,
                                        AiSummarizationService aiSummarizationService) {
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
        Page<AustraliaUpdate> updates = (status != null)
                ? australiaUpdateRepository.findByStatus(status, pageable)
                : australiaUpdateRepository.findAll(pageable);

        return updates.map(u -> new AdminUpdateSummaryResponse(
                u.getId(), u.getTitle(), u.getStatus(), u.isAiGenerated(),
                u.getCategory() != null, !u.getSources().isEmpty(),
                u.getGeographicScope() != null && !u.getGeographicScope().isBlank(),
                u.getCategory() == null ? null : u.getCategory().getId(),
                u.getGeographicScope(),
                // The reviewer needs their own draft, the raw source text to
                // write it from, and a link to the original.
                u.getKoreanSummary(),
                u.getExtractedText(),
                u.getSources().stream().findFirst()
                        .map(s -> s.getSourceUrl()).orElse(null)
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

    private AustraliaUpdateDetailResponse toDetail(AustraliaUpdate u) {
        List<SourceReferenceResponse> sources = u.getSources().stream()
                .map(s -> new SourceReferenceResponse(
                        s.getId(), s.getSource().getName(), s.getSourceUrl(),
                        s.getSourceTitle(), s.getAccessedAt()))
                .toList();

        UpdateCategoryResponse category = u.getCategory() == null ? null : new UpdateCategoryResponse(
                u.getCategory().getId(), u.getCategory().getName(), u.getCategory().getSlug());

        // Deliberately omits extractedText: this response serves the public
        // detail endpoint as well as the admin one.
        return new AustraliaUpdateDetailResponse(
                u.getId(), u.getTitle(), u.getKoreanSummary(), category,
                u.getGeographicScope(), u.getStatus(), u.isAiGenerated(), sources, u.getCreatedAt()
        );
    }
}