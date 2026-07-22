package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.*;
import com.dak.backend.service.AdminAustraliaUpdateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PatchMapping("/{updateId}/status")
    public ApiResponse<AustraliaUpdateDetailResponse> updateStatus(@PathVariable UUID updateId,
                                                                     @Valid @RequestBody UpdateAustraliaUpdateStatusRequest request) {
        return ApiResponse.ok(adminAustraliaUpdateService.updateStatus(updateId, request));
    }
}