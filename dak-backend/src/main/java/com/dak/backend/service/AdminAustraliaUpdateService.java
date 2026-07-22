package com.dak.backend.service;

import com.dak.backend.domain.AustraliaUpdate;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.AustraliaUpdateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<AdminUpdateSummaryResponse> listAll(String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        Page<AustraliaUpdate> updates = (status != null)
                ? australiaUpdateRepository.findByStatus(status, pageable)
                : australiaUpdateRepository.findAll(pageable);

        return updates.map(u -> new AdminUpdateSummaryResponse(
                u.getId(), u.getTitle(), u.getStatus(), u.isAiGenerated(),
                u.getCategory() != null, !u.getSources().isEmpty()
        ));
    }

    @Transactional
    public ImportUpdateResponse importFromUrl(ImportUpdateRequest request) {
        UrlContentFetcher.FetchedContent content = urlContentFetcher.fetch(request.sourceUrl());

        String title = content.title().isBlank() ? "Untitled (review required)" : content.title();
        String draftSummary = aiSummarizationService.summarize(title, content.bodyText());

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