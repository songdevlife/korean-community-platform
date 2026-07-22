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

import java.util.List;
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
    public Page<AustraliaUpdateSummaryResponse> search(UUID categoryId, String keyword, Pageable pageable) {
        return australiaUpdateRepository.search("PUBLISHED", categoryId, keyword, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AustraliaUpdateDetailResponse getById(UUID id) {
        AustraliaUpdate update = australiaUpdateRepository.findById(id)
                .filter(u -> "PUBLISHED".equals(u.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Australia Update not found."));

        return toDetail(update);
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
        return new AustraliaUpdateSummaryResponse(
                u.getId(), u.getTitle(), toCategoryResponse(u.getCategory()),
                u.getGeographicScope(), u.isAiGenerated(), u.getCreatedAt()
        );
    }

    private AustraliaUpdateDetailResponse toDetail(AustraliaUpdate u) {
        List<SourceReferenceResponse> sources = u.getSources().stream()
                .map(s -> new SourceReferenceResponse(
                        s.getId(), s.getSource().getName(), s.getSourceUrl(),
                        s.getSourceTitle(), s.getAccessedAt()))
                .toList();

        return new AustraliaUpdateDetailResponse(
                u.getId(), u.getTitle(), u.getKoreanSummary(), toCategoryResponse(u.getCategory()),
                u.getGeographicScope(), u.getStatus(), u.isAiGenerated(), sources, u.getCreatedAt()
        );
    }

    private UpdateCategoryResponse toCategoryResponse(UpdateCategory c) {
        return new UpdateCategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}