package com.dak.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Only title, suburb, listing type and rent are required.
 *
 * Advertisements are often thin - a photograph and three lines - and
 * demanding a bond figure or a lease term would mean either inventing one or
 * not listing the property.
 *
 * contact and imageUrls are refused unless consentStatus is FULL. The service
 * rejects them rather than silently dropping them: a listing that quietly
 * lost the advertiser's number would look like a bug, and one that quietly
 * kept it would be a breach.
 */
public record CreateRentalRequest(
        @NotBlank(message = "Enter a title.")
        @Size(max = 300, message = "Title must be 300 characters or fewer.")
        String title,

        @Size(max = 200, message = "Slug must be 200 characters or fewer.")
        String slug,

        String description,

        @NotBlank(message = "Enter a suburb.")
        @Size(max = 120) String suburb,

        @NotBlank(message = "Choose a listing type.")
        String listingType,

        @Size(max = 120) String roomTypes,

        @NotNull(message = "Enter the weekly rent.")
        @Min(value = 0, message = "Rent cannot be negative.")
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

        @Size(max = 20) String contactLanguage,

        @NotBlank(message = "Record what permission was given.")
        String consentStatus,
        String consentNote,
        @Size(max = 500) String sourceUrl,

        @Size(max = 300) String contact,

        @Size(max = 10, message = "You can add up to 10 images.")
        List<String> imageUrls
) {}