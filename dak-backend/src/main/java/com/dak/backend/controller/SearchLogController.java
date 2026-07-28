package com.dak.backend.controller;

import com.dak.backend.dto.CreateSearchLogRequest;
import com.dak.backend.service.SearchLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open to anyone, because most searches happen before anyone signs in and
 * those are the ones worth knowing about.
 *
 * Returns 204 rather than the ApiResponse envelope used elsewhere: there is no
 * resource to return, the caller ignores the response, and wrapping null in an
 * envelope would only invite the frontend to check it.
 */
@RestController
@RequestMapping("/api/v1/search-logs")
public class SearchLogController {

    private final SearchLogService searchLogService;

    public SearchLogController(SearchLogService searchLogService) {
        this.searchLogService = searchLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody CreateSearchLogRequest request) {
        searchLogService.record(request);
    }
}