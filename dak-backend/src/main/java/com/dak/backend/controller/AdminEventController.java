package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.CreateEventRequest;
import com.dak.backend.dto.EventResponse;
import com.dak.backend.dto.UpdateEventRequest;
import com.dak.backend.service.AdminEventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * /api/v1/admin/** is already restricted to ADMINISTRATOR in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    public ApiResponse<Page<EventResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminEventService.listAll(status, page, pageSize));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> detail(@PathVariable UUID eventId) {
        return ApiResponse.ok(adminEventService.getById(eventId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.ok(adminEventService.create(request));
    }

    @PatchMapping("/{eventId}")
    public ApiResponse<EventResponse> update(@PathVariable UUID eventId,
                                              @Valid @RequestBody UpdateEventRequest request) {
        return ApiResponse.ok(adminEventService.update(eventId, request));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId) {
        adminEventService.delete(eventId);
    }
}