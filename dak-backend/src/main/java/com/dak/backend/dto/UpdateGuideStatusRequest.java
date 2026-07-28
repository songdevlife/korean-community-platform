package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGuideStatusRequest(@NotBlank String status) {}