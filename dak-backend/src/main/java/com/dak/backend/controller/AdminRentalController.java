package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.AdminRentalSummaryResponse;
import com.dak.backend.dto.CreateRentalRequest;
import com.dak.backend.dto.RentalResponse;
import com.dak.backend.dto.UpdateRentalRequest;
import com.dak.backend.service.AdminRentalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Under /api/v1/admin/**, which SecurityConfig already restricts to
 * administrators - so there is no per-method guard here and none is needed.
 */
@RestController
@RequestMapping("/api/v1/admin/rentals")
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

    public AdminRentalController(AdminRentalService adminRentalService) {
        this.adminRentalService = adminRentalService;
    }

    @GetMapping
    public ApiResponse<Page<AdminRentalSummaryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminRentalService.listAll(status, page, pageSize));
    }

    @GetMapping("/{rentalId}")
    public ApiResponse<RentalResponse> detail(@PathVariable UUID rentalId) {
        return ApiResponse.ok(adminRentalService.getById(rentalId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RentalResponse> create(@Valid @RequestBody CreateRentalRequest request) {
        return ApiResponse.ok(adminRentalService.create(request));
    }

    @PatchMapping("/{rentalId}")
    public ApiResponse<RentalResponse> update(@PathVariable UUID rentalId,
                                               @Valid @RequestBody UpdateRentalRequest request) {
        return ApiResponse.ok(adminRentalService.update(rentalId, request));
    }

    /** Grants another twenty-one days, for an advertiser who is still looking. */
    @PostMapping("/{rentalId}/extend")
    public ApiResponse<RentalResponse> extend(@PathVariable UUID rentalId) {
        return ApiResponse.ok(adminRentalService.extend(rentalId));
    }

    @DeleteMapping("/{rentalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID rentalId) {
        adminRentalService.delete(rentalId);
    }
}