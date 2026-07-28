-- Business images (04 DB Design §8.10 media domain).
--
-- MVP stores an external URL per image rather than implementing the full
-- media_assets pipeline, which assumes object storage. The column set is
-- deliberately close to media_assets so that migrating to uploaded files
-- later means populating storage_key and leaving the rest intact.

CREATE TABLE business_images (
    id            UUID         PRIMARY KEY,
    business_id   UUID         NOT NULL,
    image_url     TEXT         NOT NULL,
    alt_text      VARCHAR(300),
    -- Lower numbers appear first; the lowest doubles as the card thumbnail.
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_business_images_business
        FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
);

-- Detail pages always fetch a business's images in display order.
CREATE INDEX idx_business_images_business_order
    ON business_images (business_id, display_order);