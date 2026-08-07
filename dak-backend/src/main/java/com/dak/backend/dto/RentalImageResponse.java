package com.dak.backend.dto;

import java.util.UUID;

public record RentalImageResponse(
        UUID id,
        String imageUrl,
        String altText,
        int displayOrder
) {}