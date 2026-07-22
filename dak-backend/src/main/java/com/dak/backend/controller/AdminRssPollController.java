package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.service.RssPollingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/australia-updates")
public class AdminRssPollController {

    private final RssPollingService rssPollingService;

    public AdminRssPollController(RssPollingService rssPollingService) {
        this.rssPollingService = rssPollingService;
    }

    @PostMapping("/poll-now")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, String>> pollNow() {
        rssPollingService.pollAllFeeds();
        return ApiResponse.ok(Map.of("message", "RSS polling triggered manually."));
    }
}