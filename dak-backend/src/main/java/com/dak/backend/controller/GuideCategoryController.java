package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.GuideCategoryResponse;
import com.dak.backend.repository.GuideCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// No service layer, matching UpdateCategoryController: a GET-all with no logic.
@RestController
@RequestMapping("/api/v1/guide-categories")
public class GuideCategoryController {

    private final GuideCategoryRepository guideCategoryRepository;

    public GuideCategoryController(GuideCategoryRepository guideCategoryRepository) {
        this.guideCategoryRepository = guideCategoryRepository;
    }

    @GetMapping
    public ApiResponse<List<GuideCategoryResponse>> list() {
        return ApiResponse.ok(guideCategoryRepository.findAll().stream()
                .map(c -> new GuideCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList());
    }
}