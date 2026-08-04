package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
/**
 * One event in full, for the detail page.
 *
 * Carries organiserContact, which EventSummaryResponse does not. The listing
 * has no use for it and a scraper reading one page gets one contact rather
 * than the whole table - which is also what makes it possible to tell an
 * organiser their details are not being published in bulk.
 *
 * hasPassed is computed rather than stored: a page reached from a link shared
 * weeks ago needs to say the event is over rather than present it as upcoming.
 * Images arrives in display order, and the lowest is what the card shows as
 * its thumbnail. Ordering is enforced on the entity rather than left to the
 * database, so the same image is the thumbnail on every request.
 */
public record EventResponse(
        UUID id,
        String title,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String venueName,
        String venueAddress,
        boolean isFree,
        String priceNote,
        String organiser,
        String organiserContact,
        String sourceUrl,
        List<EventImageResponse> images,
        EventCategoryResponse category,
        String status,
        boolean hasPassed,
        OffsetDateTime createdAt
) {}