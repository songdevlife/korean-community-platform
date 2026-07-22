package com.dak.backend.dto;

import java.util.UUID;

public record AdminBusinessSummaryResponse(UUID id, String name, String slug, String status) {}