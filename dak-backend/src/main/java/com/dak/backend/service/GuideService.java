package com.dak.backend.service;

import com.dak.backend.domain.Guide;
import com.dak.backend.dto.GuideCategoryResponse;
import com.dak.backend.dto.GuideDetailResponse;
import com.dak.backend.dto.GuideSummaryResponse;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.GuideRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GuideService {

    private final GuideRepository guideRepository;

    public GuideService(GuideRepository guideRepository) {
        this.guideRepository = guideRepository;
    }

    @Transactional(readOnly = true)
    public Page<GuideSummaryResponse> search(UUID categoryId, String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100),
                Sort.by(Sort.Direction.DESC, "publishedAt"));

        String normalisedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // Hard-coded to PUBLISHED: drafts and archived guides must never appear
        // in public results, regardless of query parameters.
        return guideRepository.search("PUBLISHED", categoryId, normalisedKeyword, pageable)
                .map(GuideService::toSummary);
    }

    @Transactional(readOnly = true)
    public GuideDetailResponse getBySlug(String slug) {
        Guide guide = guideRepository.findBySlugAndStatus(slug, "PUBLISHED")
                .orElseThrow(() -> ApiException.notFound("Guide not found."));
        return toDetail(guide);
    }

    // Package-private so AdminGuideService can reuse the same mapping rather
    // than maintaining a second copy that could drift.
    static GuideSummaryResponse toSummary(Guide g) {
        return new GuideSummaryResponse(
                g.getId(),
                g.getTitle(),
                g.getSlug(),
                g.getSummary(),
                toCategory(g),
                g.getPublishedAt());
    }

    static GuideDetailResponse toDetail(Guide g) {
        return new GuideDetailResponse(
                g.getId(),
                g.getTitle(),
                g.getSlug(),
                g.getSummary(),
                g.getBody(),
                toCategory(g),
                // TODO: replace with a display name once User exposes one —
                // an email address should not appear in a public response.
                g.getAuthor() == null ? null : g.getAuthor().getEmail(),
                g.getStatus(),
                g.getPublishedAt(),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }

    private static GuideCategoryResponse toCategory(Guide g) {
        return g.getCategory() == null ? null : new GuideCategoryResponse(
                g.getCategory().getId(),
                g.getCategory().getName(),
                g.getCategory().getSlug());
    }
}