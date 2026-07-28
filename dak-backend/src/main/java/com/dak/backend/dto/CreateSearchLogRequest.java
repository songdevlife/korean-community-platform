package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No length constraint on searchTerm: an over-long term is truncated by the
 * service rather than rejected. A 400 here would surface as a console error on
 * a page that otherwise worked, for a request the reader never made.
 */
public record CreateSearchLogRequest(
        @NotBlank String searchTerm,
        @NotNull Integer resultCount
) {}