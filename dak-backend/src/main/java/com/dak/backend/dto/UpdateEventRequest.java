package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Every field optional: an edit usually corrects one thing, and requiring the
 * whole record back means the caller can silently blank a field it did not
 * know about. Null means "leave alone" throughout.
 */
public record UpdateEventRequest(
        @Size(max = 300) String title,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        @Size(max = 200) String venueName,
        @Size(max = 300) String venueAddress,
        Boolean isFree,
        @Size(max = 100) String priceNote,
        @Size(max = 200) String organiser,
        @Size(max = 300) String organiserContact,
        @Size(max = 500) String sourceUrl,
        UUID categoryId,
        String status
) {}