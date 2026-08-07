package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * The public address. Matches guides at 320 characters so the two can
     * share validation, and unique so a link resolves to one event.
     *
     * Set once, at creation, and not derived from startsAt afterwards: an
     * event whose date is corrected keeps the URL it was shared under.
     */
    @Column(nullable = false, unique = true, length = 320)
    private String slug;

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

    /**
     * Posters and photographs, in display order. The lowest doubles as the
     * card thumbnail, so the ordering is not cosmetic.
     */
    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<EventImage> images = new ArrayList<>();

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