package com.dak.backend.dto;

import java.util.UUID;

public record BusinessCategoryResponse(UUID id, String name, String slug) {}