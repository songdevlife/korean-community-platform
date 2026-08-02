package com.dak.backend.dto;

import java.util.UUID;

public record EventCategoryResponse(
        UUID id,
        String name,
        String slug
) {}