package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.BusinessDetailResponse;
import com.dak.backend.dto.BusinessSummaryResponse;
import com.dak.backend.dto.CreateBusinessRequest;
import com.dak.backend.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping
    public ApiResponse<Page<BusinessSummaryResponse>> search(
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100));
        return ApiResponse.ok(businessService.search(suburb, category, keyword, pageable));
    }

    @GetMapping("/{slug}")
    public ApiResponse<BusinessDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(businessService.getBySlug(slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BusinessDetailResponse> create(@Valid @RequestBody CreateBusinessRequest request) {
        return ApiResponse.ok(businessService.create(request));
    }
}