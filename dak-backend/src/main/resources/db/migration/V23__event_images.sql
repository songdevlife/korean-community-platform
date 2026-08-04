-- Event images.
--
-- V22 added a single image_url to events, on the assumption that an event has
-- at most one poster. An organiser who sends three photographs of a venue
-- proved otherwise. This table replaces that column and mirrors
-- business_images exactly, so the gallery component can be shared rather than
-- copied.

CREATE TABLE event_images (
    id            UUID         PRIMARY KEY,
    event_id      UUID         NOT NULL,
    image_url     TEXT         NOT NULL,
    alt_text      VARCHAR(300),
    -- Lower numbers appear first; the lowest doubles as the card thumbnail.
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_event_images_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

-- Detail pages always fetch an event's images in display order.
CREATE INDEX idx_event_images_event_order
    ON event_images (event_id, display_order);

-- Carry V22's single poster across before the column goes. Runs before the
-- DROP deliberately: reversing these two statements loses the URL, and a
-- migration cannot be un-applied.
INSERT INTO event_images (id, event_id, image_url, display_order)
SELECT gen_random_uuid(), id, image_url, 0
FROM events
WHERE image_url IS NOT NULL AND image_url <> '';

ALTER TABLE events DROP COLUMN image_url;