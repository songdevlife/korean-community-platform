package com.dak.backend.dto;

import java.util.UUID;

public record GuideCategoryResponse(UUID id, String name, String slug) {}