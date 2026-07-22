package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityCommentResponse(
        UUID id,
        UUID parentCommentId,
        String content,
        CommunityPostResponse.AuthorSummary author,
        OffsetDateTime createdAt
) {}