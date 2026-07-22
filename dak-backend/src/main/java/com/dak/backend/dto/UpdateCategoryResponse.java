package com.dak.backend.dto;

import java.util.UUID;

public record UpdateCategoryResponse(UUID id, String name, String slug) {}