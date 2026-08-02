package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A gathering readers can go to.
 *
 * Entered by hand. Facebook and Instagram publish no feed and refuse crawlers,
 * so unlike Australia Updates there is no import pipeline to build - and the
 * permission conversation with an organiser would be manual regardless, which
 * is where the contact details and the poster permission come from.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The field that makes this table different from every other one: an event
     * that has happened is not old content, it is wrong content. Listings
     * filter on this rather than deleting, so a past event keeps its URL.
     */
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "venue_address", length = 300)
    private String venueAddress;

    /**
     * Carried separately from priceNote so that "free" is something the listing
     * knows rather than infers from a missing number.
     */
    @Column(name = "is_free", nullable = false)
    private boolean isFree = false;

    @Column(name = "price_note", length = 100)
    private String priceNote;

    @Column(length = 200)
    private String organiser;

    /**
     * A third party's contact details, copied from a post they made to be found
     * by. Included only where the organiser has been asked, and removed the
     * moment they ask - see content-policy.md.
     */
    @Column(name = "organiser_contact", length = 300)
    private String organiserContact;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EventCategory category;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static Event createNew(String title, OffsetDateTime startsAt) {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = new Event();
        event.title = title;
        event.startsAt = startsAt;
        event.status = "DRAFT";
        event.createdAt = now;
        event.updatedAt = now;
        return event;
    }
}