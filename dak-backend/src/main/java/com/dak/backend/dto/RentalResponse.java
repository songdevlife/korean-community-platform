package com.dak.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One rental in full, for the detail page.
 *
 * Carries contact, which the summary does not, and which is null unless the
 * advertiser agreed to it being published. consentNote never appears here at
 * all: it records when and how permission was given and is for DAK's own
 * files, not for readers.
 *
 * tenancyStatus is computed rather than stored. Two or more rooms let makes a
 * property a rooming house under Part 7 of the Residential Tenancies Act,
 * with a bond that must be lodged and sixty days' notice to leave; one room
 * let in an owner-occupied house falls outside the Act entirely. Most
 * advertisements do not say which, so UNKNOWN is the common answer and is
 * itself worth printing - a reader who knows to ask is better off than one
 * who does not know the question exists.
 */
public record RentalResponse(
        UUID id,
        String slug,
        String title,
        String description,
        String suburb,
        String listingType,
        String roomTypes,
        Integer rentMin,
        Integer rentMax,
        BigDecimal bondWeeks,
        String billsIncluded,
        String billsNote,
        LocalDate availableFrom,
        Integer minTermMonths,
        Boolean furnished,
        Integer roomsLet,
        String tenancyStatus,
        String genderPreference,
        Boolean couplesAllowed,
        Boolean petsAllowed,
        Boolean smokingAllowed,
        String inspectionNote,
        // KOREAN, ENGLISH, BOTH or UNKNOWN. The page shows nothing for
        // UNKNOWN: a reader is better served by silence than by a badge
        // saying DAK does not know.
        String contactLanguage,
        String consentStatus,
        String sourceUrl,
        // Null for consented listings. Printed on external ones, where a
        // reader has no other way to judge how old the information is.
        OffsetDateTime lastCheckedAt,
        String contact,
        List<RentalImageResponse> images,
        String status,
        boolean hasExpired,
        OffsetDateTime createdAt
) {}