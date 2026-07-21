package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/v1/health
 * Sanity-check endpoint to confirm the app boots and the standard response
 * envelope (05_API_Specification_DAK.docx 11.2) is wired correctly.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "dak-backend"));
    }
}
