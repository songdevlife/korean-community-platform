package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.RentalResponse;
import com.dak.backend.dto.RentalSummaryResponse;
import com.dak.backend.service.RentalService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    /**
     * No sort parameter. Newest first is the only order that makes sense for
     * listings that expire in three weeks - sorting by price would put the
     * cheapest room from a fortnight ago above one posted this morning, and
     * the one posted this morning is the one still available.
     */
    @GetMapping
    public ApiResponse<Page<RentalSummaryResponse>> list(
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer maxRent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(rentalService.listCurrent(suburb, type, maxRent, page, pageSize));
    }

    /**
     * String rather than UUID: Spring converts the path variable before the
     * method runs, so a UUID parameter would reject every slug with a 400.
     */
    @GetMapping("/{identifier}")
    public ApiResponse<RentalResponse> detail(@PathVariable String identifier) {
        return ApiResponse.ok(rentalService.getByIdentifier(identifier));
    }
}