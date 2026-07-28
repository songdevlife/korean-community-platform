package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ImportUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "sourceUrl must start with http:// or https://")
        String sourceUrl,

        /**
         * The publisher this article came from. Required rather than derived from
         * the URL's domain: an unregistered domain would silently produce an
         * update with no attribution, and 03 MVP 12 requires a reader to be able
         * to reach the original. Publication is blocked without a source anyway,
         * so demanding it at import saves a step rather than adding one.
         */
        @NotNull UUID sourceId
) {}