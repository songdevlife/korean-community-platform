package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.AustraliaUpdateDetailResponse;
import com.dak.backend.dto.AustraliaUpdateSummaryResponse;
import com.dak.backend.dto.CreateAustraliaUpdateRequest;
import com.dak.backend.service.AustraliaUpdateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/australia-updates")
public class AustraliaUpdateController {

    private final AustraliaUpdateService australiaUpdateService;

    public AustraliaUpdateController(AustraliaUpdateService australiaUpdateService) {
        this.australiaUpdateService = australiaUpdateService;
    }

    @GetMapping
    public ApiResponse<Page<AustraliaUpdateSummaryResponse>> search(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        return ApiResponse.ok(australiaUpdateService.search(category, keyword, pageable));
    }

    @GetMapping("/{updateId}")
    public ApiResponse<AustraliaUpdateDetailResponse> getById(@PathVariable UUID updateId) {
        return ApiResponse.ok(australiaUpdateService.getById(updateId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AustraliaUpdateDetailResponse> create(@Valid @RequestBody CreateAustraliaUpdateRequest request) {
        return ApiResponse.ok(australiaUpdateService.create(request));
    }
}