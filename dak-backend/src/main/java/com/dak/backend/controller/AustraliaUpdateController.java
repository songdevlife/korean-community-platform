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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/australia-updates")
public class AustraliaUpdateController {

    // Whitelist of sortable fields. Sort values arrive from the client and are
    // interpolated into a JPQL order-by, so an unchecked value would let a
    // caller sort by arbitrary entity properties.
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "title");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    // Mirrors the ck_australia_updates_scope constraint, so an invalid value is
    // rejected with a clear message rather than silently matching nothing.
    private static final Set<String> VALID_SCOPES = Set.of(
            "ADELAIDE", "SOUTH_AUSTRALIA", "AUSTRALIA", "COUNCIL_AREA", "SUBURB");

    private final AustraliaUpdateService australiaUpdateService;

    public AustraliaUpdateController(AustraliaUpdateService australiaUpdateService) {
        this.australiaUpdateService = australiaUpdateService;
    }

    @GetMapping
    public ApiResponse<Page<AustraliaUpdateSummaryResponse>> search(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String normalisedScope = normaliseScope(scope);
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 100), parseSort(sort));
        return ApiResponse.ok(
                australiaUpdateService.search(category, normalisedScope, keyword, pageable));
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

    /**
     * Accepts scope case-insensitively and treats an unrecognised value as no
     * filter — a stale bookmark should return everything rather than nothing.
     */
    private String normaliseScope(String scope) {
        if (scope == null || scope.isBlank()) return null;
        String upper = scope.trim().toUpperCase();
        return VALID_SCOPES.contains(upper) ? upper : null;
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