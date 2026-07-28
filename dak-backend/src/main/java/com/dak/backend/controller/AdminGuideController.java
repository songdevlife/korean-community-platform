package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.User;
import com.dak.backend.dto.AdminGuideSummaryResponse;
import com.dak.backend.dto.CreateGuideRequest;
import com.dak.backend.dto.GuideDetailResponse;
import com.dak.backend.dto.UpdateGuideRequest;
import com.dak.backend.dto.UpdateGuideStatusRequest;
import com.dak.backend.service.AdminGuideService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only Guide management. No SecurityConfig rule is needed here because
 * /api/v1/admin/** is already restricted to ADMINISTRATOR.
 */
@RestController
@RequestMapping("/api/v1/admin/guides")
public class AdminGuideController {

    private final AdminGuideService adminGuideService;

    public AdminGuideController(AdminGuideService adminGuideService) {
        this.adminGuideService = adminGuideService;
    }

    /** Returns every status, unlike the public endpoint which is PUBLISHED-only. */
    @GetMapping
    public ApiResponse<Page<AdminGuideSummaryResponse>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminGuideService.listAll(status, page, pageSize));
    }

    /**
     * The author is taken from the authenticated principal rather than the
     * request body, so a guide cannot be attributed to another user.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GuideDetailResponse> create(
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreateGuideRequest request) {
        return ApiResponse.ok(adminGuideService.create(request, author));
    }

    @PatchMapping("/{guideId}")
    public ApiResponse<GuideDetailResponse> update(
            @PathVariable UUID guideId,
            @Valid @RequestBody UpdateGuideRequest request) {
        return ApiResponse.ok(adminGuideService.update(guideId, request));
    }

    /** Handles DRAFT, PUBLISHED and ARCHIVED transitions; publication rules are enforced in the service. */
    @PatchMapping("/{guideId}/status")
    public ApiResponse<GuideDetailResponse> updateStatus(
            @PathVariable UUID guideId,
            @Valid @RequestBody UpdateGuideStatusRequest request) {
        return ApiResponse.ok(adminGuideService.updateStatus(guideId, request));
    }
}