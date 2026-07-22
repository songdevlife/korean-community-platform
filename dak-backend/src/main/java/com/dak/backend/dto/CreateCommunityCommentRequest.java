package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommunityCommentRequest(
        @NotBlank String content,
        UUID parentCommentId
) {}