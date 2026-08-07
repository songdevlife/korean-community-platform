package com.dak.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One rental in the admin queue.
 *
 * Carries consentStatus and consentNote, which no public response does: the
 * queue is where an administrator checks whether a listing may show a number
 * before publishing it, and the note is the record of the DM that said so.
 */
public record AdminRentalSummaryResponse(
        UUID id,
        String slug,
        String title,
        String suburb,
        Integer rentMin,
        String status,
        String consentStatus,
        String consentNote,
        String sourceUrl,
        boolean hasContact,
        int imageCount,
        OffsetDateTime expiresAt,
        boolean hasExpired,
        OffsetDateTime createdAt
) {}