package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.BusinessCategoryResponse;
import com.dak.backend.repository.BusinessCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business-categories")
public class BusinessCategoryController {

    private final BusinessCategoryRepository businessCategoryRepository;

    public BusinessCategoryController(BusinessCategoryRepository businessCategoryRepository) {
        this.businessCategoryRepository = businessCategoryRepository;
    }

    @GetMapping
    public ApiResponse<List<BusinessCategoryResponse>> getCategories() {
        List<BusinessCategoryResponse> categories = businessCategoryRepository.findAll().stream()
                .map(c -> new BusinessCategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();

        return ApiResponse.ok(categories);
    }
}