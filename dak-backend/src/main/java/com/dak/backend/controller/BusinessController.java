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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    // Whitelist of sortable fields. Sort values arrive from the client and are
    // interpolated into a JPQL order-by, so an unchecked value would let a
    // caller sort by arbitrary entity properties.
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "createdAt", "suburb");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Serves both the directory listing and business search — 05 API Spec 9.2
     * notes the two may share logic, and splitting them would duplicate every
     * filter. Parameters not yet supported (openNow, minimumRating, language)
     * depend on data the schema does not carry: see D6.
     */
    @GetMapping
    public ApiResponse<Page<BusinessSummaryResponse>> search(
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            // Boxed so an absent parameter means "don't filter" rather than false.
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), parseSort(sort));
        return ApiResponse.ok(businessService.search(suburb, category, keyword, verified, pageable));
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

    /**
     * Parses a "field,direction" sort parameter, falling back to newest-first
     * when the field is not whitelisted or the value is malformed.
     */
    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();

        if (!SORTABLE_FIELDS.contains(field)) {
            field = DEFAULT_SORT_FIELD;
        }

        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }
}