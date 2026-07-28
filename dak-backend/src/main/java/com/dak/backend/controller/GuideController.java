package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.GuideDetailResponse;
import com.dak.backend.dto.GuideSummaryResponse;
import com.dak.backend.service.GuideService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guides")
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    @GetMapping
    public ApiResponse<Page<GuideSummaryResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(guideService.search(categoryId, keyword, page, pageSize));
    }

    // Slug rather than UUID: guide URLs are a search-visibility surface.
    @GetMapping("/{slug}")
    public ApiResponse<GuideDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(guideService.getBySlug(slug));
    }
}