package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One source behind an Australia Update, as the detail page shows it.
 *
 * licenceName and licenceUrl are null for ordinary copyright sources and
 * carry a value only where the publisher licenses its material openly. CC BY
 * 4.0 asks for a link to the licence alongside the credit, so where these are
 * present the page is obliged to render them; where they are absent, printing
 * a licence line would be a false claim about the source.
 */
public record SourceReferenceResponse(
        UUID id,
        String sourceName,
        String sourceUrl,
        String sourceTitle,
        String licenceName,
        String licenceUrl,
        OffsetDateTime accessedAt
) {}