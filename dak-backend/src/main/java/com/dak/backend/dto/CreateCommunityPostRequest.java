package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCommunityPostRequest(
        @NotBlank
        @Pattern(regexp = "GENERAL|QUESTIONS|BUY_AND_SELL|JOBS|EVENTS")
        String category,

        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}