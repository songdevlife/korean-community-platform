package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.EventCategoryResponse;
import com.dak.backend.dto.EventResponse;
import com.dak.backend.dto.EventSummaryResponse;
import com.dak.backend.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * No sort parameter, unlike businesses and updates. There is one useful
     * order for a list of things that are about to happen, and offering
     * another would mostly offer a way to get it wrong.
     */
    @GetMapping
    public ApiResponse<Page<EventSummaryResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(eventService.listUpcoming(category, page, pageSize));
    }

    @GetMapping("/categories")
    public ApiResponse<List<EventCategoryResponse>> categories() {
        return ApiResponse.ok(eventService.listCategories());
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> detail(@PathVariable UUID eventId) {
        return ApiResponse.ok(eventService.getById(eventId));
    }
}