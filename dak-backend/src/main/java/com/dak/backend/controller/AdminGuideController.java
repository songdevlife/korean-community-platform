package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.User;
import com.dak.backend.dto.AdminGuideSummaryResponse;
import com.dak.backend.dto.CreateGuideRequest;
import com.dak.backend.dto.GuideDetailResponse;
import com.dak.backend.dto.UpdateGuideRequest;
import com.dak.backend.dto.CardSpec;
import com.dak.backend.dto.UpdateGuideStatusRequest;
import com.dak.backend.service.AdminGuideService;
import com.dak.backend.service.CardRendererService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /**
     * The text and layout decisions behind the card, without rendering it.
     * Cheap compared with rendering, since no illustration is generated.
     */
    @PostMapping("/{guideId}/card-preview")
    public ApiResponse<CardSpec> generateCardPreview(
            @PathVariable UUID guideId) {
        return ApiResponse.ok(adminGuideService.generateCardSpec(guideId));
    }

    /**
     * Renders one card. Index 0 is the cover; later indexes are the cards of
     * a carousel, which guides produce more often than news does — a guide is
     * usually a sequence rather than a single fact.
     *
     * The card count travels as a header because it cannot be read from a
     * PNG. It is exposed to the browser in CorsConfig; without that the
     * header is dropped before it arrives.
     */
    @PostMapping(
            value = "/{guideId}/card-render-preview",
            produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> renderCard(
            @PathVariable UUID guideId,
            @RequestParam(defaultValue = "false") boolean regenerate,
            @RequestParam(defaultValue = "0") int index) {

        CardRendererService.RenderedCard rendered =
                adminGuideService.generateCardPreview(guideId, regenerate, index);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(
                        "X-Card-Count",
                        String.valueOf(adminGuideService.countCards(guideId)))
                .body(rendered.imageBytes());
    }
}