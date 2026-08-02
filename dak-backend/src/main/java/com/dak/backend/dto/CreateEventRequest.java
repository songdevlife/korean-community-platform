package com.dak.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Only the title and start time are required.
 *
 * Events are transcribed from posts that are often thin - a Facebook message
 * saying where and when and nothing else. Demanding a description or a venue
 * would mean either inventing one or not listing the event.
 */
public record CreateEventRequest(
        @NotBlank(message = "Enter a title.")
        @Size(max = 300, message = "Title must be 300 characters or fewer.")
        String title,

        String description,

        @NotNull(message = "Enter a start time.")
        OffsetDateTime startsAt,

        OffsetDateTime endsAt,

        @Size(max = 200) String venueName,
        @Size(max = 300) String venueAddress,

        boolean isFree,
        @Size(max = 100) String priceNote,

        @Size(max = 200) String organiser,
        @Size(max = 300) String organiserContact,
        @Size(max = 500) String sourceUrl,

        UUID categoryId
) {}