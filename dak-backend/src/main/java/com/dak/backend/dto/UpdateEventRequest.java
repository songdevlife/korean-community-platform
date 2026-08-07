package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Every field optional: an edit usually corrects one thing, and requiring the
 * whole record back means the caller can silently blank a field it did not
 * know about. Null means "leave alone" throughout.
 */
public record UpdateEventRequest(
        @Size(max = 300) String title,
        // Deliberately absent from the update path: changing a published slug
        // breaks every link already shared under it, and the UUID fallback
        // does not save an old slug. Same hazard AdminGuideService records.
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
        // Null leaves images alone; an empty list removes them all.
        @Size(max = 10, message = "You can add up to 10 images.")
        List<String> imageUrls,
        UUID categoryId,
        String status
) {}