package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.UpdateBusinessStatusRequest;
import com.dak.backend.service.AdminBusinessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/businesses")
public class AdminBusinessController {

    private final AdminBusinessService adminBusinessService;

    public AdminBusinessController(AdminBusinessService adminBusinessService) {
        this.adminBusinessService = adminBusinessService;
    }

    @PatchMapping("/{businessId}/status")
    public ApiResponse<BusinessDetailResponse> updateStatus(@PathVariable UUID businessId,
                                                              @Valid @RequestBody UpdateBusinessStatusRequest request) {
        return ApiResponse.ok(adminBusinessService.updateStatus(businessId, request));
    }
}