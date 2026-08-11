package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.*;
import com.dak.backend.service.AdminAustraliaUpdateService;
import com.dak.backend.service.CardRendererService;
import com.dak.backend.service.HeroImageGenerationService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/admin/australia-updates")
public class AdminAustraliaUpdateController {

    private final AdminAustraliaUpdateService adminAustraliaUpdateService;

    public AdminAustraliaUpdateController(AdminAustraliaUpdateService adminAustraliaUpdateService) {
        this.adminAustraliaUpdateService = adminAustraliaUpdateService;
    }

    @GetMapping
    public ApiResponse<Page<AdminUpdateSummaryResponse>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminAustraliaUpdateService.listAll(status, page, pageSize));
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportUpdateResponse> importFromUrl(@Valid @RequestBody ImportUpdateRequest request) {
        return ApiResponse.ok(adminAustraliaUpdateService.importFromUrl(request));
    }

    @PatchMapping("/{updateId}/metadata")
    public ApiResponse<AustraliaUpdateDetailResponse> updateMetadata(
            @PathVariable UUID updateId,
            @Valid @RequestBody UpdateAustraliaUpdateMetadataRequest request) {
        return ApiResponse.ok(adminAustraliaUpdateService.updateMetadata(updateId, request));
    }

    @PatchMapping("/{updateId}/status")
    public ApiResponse<AustraliaUpdateDetailResponse> updateStatus(@PathVariable UUID updateId,
                                                                     @Valid @RequestBody UpdateAustraliaUpdateStatusRequest request) {
        return ApiResponse.ok(adminAustraliaUpdateService.updateStatus(updateId, request));
    }

    @PostMapping("/{updateId}/card-preview")
    public ApiResponse<CardSpec> generateCardPreview(
            @PathVariable UUID updateId
    ) {
        return ApiResponse.ok(
                adminAustraliaUpdateService.generateCardSpec(updateId)
        );
    }

    /**
     * Renders and stores the cover card, returning where it now lives.
     */
    @PostMapping("/{updateId}/card")
    public ApiResponse<CardAssetResponse> saveCard(
            @PathVariable UUID updateId
    ) {
        com.dak.backend.domain.CardAsset asset =
                adminAustraliaUpdateService.saveCard(updateId);

        return ApiResponse.ok(
                new CardAssetResponse(
                        asset.getImageUrl(),
                        asset.getLayoutType(),
                        asset.getCreatedAt()
                )
        );
    }

    public record CardAssetResponse(
            String imageUrl,
            String layoutType,
            java.time.OffsetDateTime createdAt
    ) {}

    @PostMapping("/{updateId}/hero-preview")
    public ApiResponse<HeroImageGenerationService.HeroImageResult> generateHeroPreview(
            @PathVariable UUID updateId
    ) {
        return ApiResponse.ok(
                adminAustraliaUpdateService.generateHeroPreview(updateId)
        );
    }

    /**
     * Renders one card.
     *
     * Index 0 is the cover; later indexes are the cards of a carousel, where
     * the content produced one. The hero illustration is reused between
     * requests where the inputs have not changed, because generating one is a
     * paid call — pass regenerate=true to discard it and produce new artwork.
     *
     * The number of cards is not knowable from a PNG, so it travels as a
     * header: the admin screen needs it to decide whether to offer a next
     * card. It is exposed to the browser in SecurityConfig's CORS
     * configuration; without that the header is dropped before it arrives.
     */
    @PostMapping(
            value = "/{updateId}/card-render-preview",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> generateFinalCardPreview(
            @PathVariable UUID updateId,
            @RequestParam(defaultValue = "false") boolean regenerate,
            @RequestParam(defaultValue = "0") int index
    ) {
        CardRendererService.RenderedCard rendered =
                adminAustraliaUpdateService.generateFinalCardPreview(
                        updateId,
                        regenerate,
                        index
                );

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(
                        "X-Card-Count",
                        String.valueOf(adminAustraliaUpdateService.countCards(updateId))
                )
                .body(rendered.imageBytes());
    }
}
