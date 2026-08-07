package com.dak.backend.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Every field optional; null means "leave alone".
 *
 * No slug: changing a published address breaks every link already shared
 * under it, which is the rule events enforce the same way.
 *
 * consentStatus is editable, because permission arrives after a listing goes
 * up as often as before. Lowering it from FULL clears the contact and the
 * images in the same operation - an advertiser who withdraws permission has
 * withdrawn it from everything, not just from the field named in the request.
 */
public record UpdateRentalRequest(
        @Size(max = 300) String title,
        String description,
        @Size(max = 120) String suburb,
        String listingType,
        @Size(max = 120) String roomTypes,
        Integer rentMin,
        Integer rentMax,
        BigDecimal bondWeeks,
        String billsIncluded,
        @Size(max = 300) String billsNote,
        LocalDate availableFrom,
        Integer minTermMonths,
        Boolean furnished,
        Integer roomsLet,
        @Size(max = 20) String genderPreference,
        Boolean couplesAllowed,
        Boolean petsAllowed,
        Boolean smokingAllowed,
        @Size(max = 300) String inspectionNote,
        String consentStatus,
        String consentNote,
        @Size(max = 500) String sourceUrl,
        @Size(max = 300) String contact,
        @Size(max = 10, message = "You can add up to 10 images.")
        List<String> imageUrls,
        String status,
        OffsetDateTime expiresAt
) {}