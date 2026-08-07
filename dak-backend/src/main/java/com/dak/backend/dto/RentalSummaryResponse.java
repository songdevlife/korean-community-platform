package com.dak.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One rental as a card in a list.
 *
 * Deliberately without contact and without the consent note: a listing shows
 * where, how much and what kind, and anyone who wants to enquire opens it
 * first. That the omission also stops a scraper collecting every advertiser's
 * number in one request is the point rather than a side effect - the same
 * reasoning EventSummaryResponse carries.
 */
public record RentalSummaryResponse(
        UUID id,
        String slug,
        String title,
        String suburb,
        String listingType,
        String roomTypes,
        Integer rentMin,
        Integer rentMax,
        String billsIncluded,
        LocalDate availableFrom,
        Boolean furnished,
        String thumbnailUrl
) {}