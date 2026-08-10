-- Generated card artwork, stored so that it survives a restart and can be
-- reached by a URL. Before this the images lived in an in-memory map, which
-- was adequate while cards were being designed and unusable for publishing.
--
-- Two kinds share the table. A HERO is the AI illustration or photograph that
-- goes inside a card; it is expensive to produce and is reused whenever the
-- inputs that describe it have not changed. A CARD is the finished 1080x1350
-- image and is what actually gets published.

CREATE TABLE card_assets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Cards are wanted for guides as well as updates, so the owner is
    -- identified by type and id rather than by a foreign key to one table.
    content_type  VARCHAR(32)  NOT NULL,
    content_id    UUID         NOT NULL,

    asset_kind    VARCHAR(16)  NOT NULL,

    -- Identifies the inputs a hero was generated from. When the card text is
    -- rewritten the description changes with it, the hash stops matching, and
    -- new artwork is generated rather than the old picture being reused for a
    -- story it no longer illustrates. Null for a finished card.
    spec_hash     VARCHAR(64),

    layout_type   VARCHAR(32),

    image_url     TEXT         NOT NULL,

    -- Cloudinary's own identifier, kept so an asset can be replaced or
    -- deleted rather than only added to.
    public_id     VARCHAR(255) NOT NULL,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_card_assets_content_type
        CHECK (content_type IN ('AU_UPDATE', 'GUIDE')),

    CONSTRAINT ck_card_assets_kind
        CHECK (asset_kind IN ('HERO', 'CARD'))
);

-- The lookup every render performs: does artwork already exist for this
-- content, of this kind, from these inputs.
CREATE INDEX ix_card_assets_lookup
    ON card_assets (content_type, content_id, asset_kind, created_at DESC);

-- One hero per set of inputs. Without this a race between two previews of the
-- same update would upload twice and pay twice.
CREATE UNIQUE INDEX ux_card_assets_hero_spec
    ON card_assets (content_type, content_id, spec_hash)
    WHERE asset_kind = 'HERO';