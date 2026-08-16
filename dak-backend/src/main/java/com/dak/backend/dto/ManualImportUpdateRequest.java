package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ManualImportUpdateRequest(

    @NotBlank
    String sourceName,

    @NotBlank
@Pattern(
        regexp = "^(OFFICIAL_GOVERNMENT|OFFICIAL_ORGANISATION|LOCAL_AUTHORITY|NEWS_MEDIA|COMMUNITY_ORGANISATION|SOCIAL_MEDIA|USER_SUBMISSION|OTHER)$",
        message = "Invalid source type"
)
String sourceType,

    @NotBlank
    String sourceTitle,

    @NotBlank
    String sourceContent,

    @Pattern(
            regexp = "^https?://.+",
            message = "sourceUrl must start with http:// or https://"
    )
    String sourceUrl
) {}