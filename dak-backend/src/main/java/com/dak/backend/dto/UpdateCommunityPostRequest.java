package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommunityPostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}