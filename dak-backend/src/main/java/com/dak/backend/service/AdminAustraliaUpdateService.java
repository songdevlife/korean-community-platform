package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.AustraliaUpdateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminAustraliaUpdateService {

    private final AustraliaUpdateRepository australiaUpdateRepository;
    private final UrlContentFetcher urlContentFetcher;
    private final AiSummarizationService aiSummarizationService;

    public AdminAustraliaUpdateService(AustraliaUpdateRepository australiaUpdateRepository,
                                        UrlContentFetcher urlContentFetcher,
                                        AiSummarizationService aiSummarizationService) {
        this.australiaUpdateRepository = australiaUpdateRepository;
        this.urlContentFetcher = urlContentFetcher;
        this.aiSummarizationService = aiSummarizationService;
    }

    @Transactional
    public ImportUpdateResponse importFromUrl(ImportUpdateRequest request) {
        UrlContentFetcher.FetchedContent content = urlContentFetcher.fetch(request.sourceUrl());

        String title = content.title().isBlank() ? "Untitled (review required)" : content.title();
        String draftSummary = aiSummarizationService.summarize(title, content.bodyText());

        // Per 05 API Spec §10.5 notes: "Imported content must remain a draft until
        // reviewed" and "Failed imports should not create incomplete public content" —
        // createDraftFromImport always starts at status=DRAFT, never PUBLISHED.
        AustraliaUpdate update = AustraliaUpdate.createDraftFromImport(title, draftSummary);
        australiaUpdateRepository.save(update);

        return new ImportUpdateResponse(
                update.getId(), request.sourceUrl(), title, draftSummary, update.getStatus());
    }

    @Transactional
    public AustraliaUpdateDetailResponse updateStatus(UUID updateId, UpdateAustraliaUpdateStatusRequest request) {
        AustraliaUpdate update = australiaUpdateRepository.findById(updateId)
                .orElseThrow(() -> ApiException.notFound("Australia Update not found."));

        if ("PUBLISHED".equals(request.status())) {
            // Per 04 Database Design §12.5: publication requires a source, plus (as of V5)
            // category and geographic scope must have been filled in by an administrator
            // since AI-imported drafts start without them.
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

        return new AustraliaUpdateDetailResponse(
                u.getId(), u.getTitle(), u.getKoreanSummary(), category,
                u.getGeographicScope(), u.getStatus(), u.isAiGenerated(), sources, u.getCreatedAt()
        );
    }
}