package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One event as a card in a list.
 *
 * Deliberately without organiserContact, and without description: a listing
 * shows when, where and how much, and anyone who wants to get in touch opens
 * the event first. That the omission also makes bulk collection of contact
 * details a page at a time is the point rather than a side effect.
 */
public record EventSummaryResponse(
        UUID id,
        String title,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String venueName,
        boolean isFree,
        String priceNote,
        EventCategoryResponse category
) {}