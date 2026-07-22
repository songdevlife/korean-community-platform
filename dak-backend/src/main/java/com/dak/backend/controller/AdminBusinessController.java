package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.AdminBusinessSummaryResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.UpdateBusinessStatusRequest;
import com.dak.backend.service.AdminBusinessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/businesses")
public class AdminBusinessController {

    private final AdminBusinessService adminBusinessService;

    public AdminBusinessController(AdminBusinessService adminBusinessService) {
        this.adminBusinessService = adminBusinessService;
    }

    @GetMapping
    public ApiResponse<Page<AdminBusinessSummaryResponse>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminBusinessService.listAll(status, page, pageSize));
    }

    @PatchMapping("/{businessId}/status")
    public ApiResponse<BusinessDetailResponse> updateStatus(@PathVariable UUID businessId,
                                                              @Valid @RequestBody UpdateBusinessStatusRequest request) {
        return ApiResponse.ok(adminBusinessService.updateStatus(businessId, request));
    }
}