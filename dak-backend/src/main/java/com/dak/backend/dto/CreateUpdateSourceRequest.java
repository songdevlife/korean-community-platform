package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUpdateSourceRequest(
        @NotBlank String name,

        @NotBlank
        @Pattern(regexp = "OFFICIAL_GOVERNMENT|OFFICIAL_ORGANISATION|LOCAL_AUTHORITY|" +
                           "NEWS_MEDIA|COMMUNITY_ORGANISATION|SOCIAL_MEDIA|USER_SUBMISSION|OTHER")
        String sourceType
) {}