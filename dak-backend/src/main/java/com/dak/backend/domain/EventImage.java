package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps to the `event_images` table.
 *
 * Mirrors BusinessImage deliberately — same columns, same lifecycle — so the
 * gallery on the frontend is one component rather than two.
 *
 * An image here is a poster the organiser has given permission to use. That
 * permission is separate from the permission to list the event: the facts of
 * an event are not copyright, the artwork is. Absent unless asked for and
 * granted, and removed on request — see content-policy.md.
 */
@Entity
@Table(name = "event_images")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventImage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static EventImage createNew(Event event, String imageUrl,
                                       String altText, int displayOrder) {
        EventImage image = new EventImage();
        image.event = event;
        image.imageUrl = imageUrl;
        image.altText = altText;
        image.displayOrder = displayOrder;
        image.createdAt = OffsetDateTime.now();
        return image;
    }
}