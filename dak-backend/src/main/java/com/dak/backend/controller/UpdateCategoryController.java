package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.UpdateCategoryResponse;
import com.dak.backend.repository.UpdateCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/update-categories")
public class UpdateCategoryController {

    private final UpdateCategoryRepository updateCategoryRepository;

    public UpdateCategoryController(UpdateCategoryRepository updateCategoryRepository) {
        this.updateCategoryRepository = updateCategoryRepository;
    }

    @GetMapping
    public ApiResponse<List<UpdateCategoryResponse>> getCategories() {
        List<UpdateCategoryResponse> categories = updateCategoryRepository.findAll().stream()
                .map(c -> new UpdateCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
        return ApiResponse.ok(categories);
    }
}