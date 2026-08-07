-- Rental listings.
--
-- Recorded from advertisements published elsewhere, or supplied by the
-- advertiser directly. The distinction is not cosmetic: consent_status
-- decides whether contact details and photographs may be stored at all,
-- and the service refuses them where it is not FULL.

CREATE TABLE rentals (
    id                  UUID PRIMARY KEY,
    slug                VARCHAR(320) NOT NULL,

    title               VARCHAR(300) NOT NULL,
    description         TEXT,

    -- Where. Suburb rather than a full address: a room advertisement rarely
    -- gives a street number, and publishing one for a house someone lives in
    -- would be worse than useless to a reader.
    suburb              VARCHAR(120) NOT NULL,

    listing_type        VARCHAR(30)  NOT NULL,
    room_types          VARCHAR(120),

    -- rent_max is set only where the advertisement gives a range, which
    -- happens where one listing covers several rooms of different sizes.
    rent_min            INTEGER      NOT NULL,
    rent_max            INTEGER,

    -- Weeks rather than dollars, because that is how advertisements state it
    -- and because the legal cap is expressed in weeks: four at or below $800
    -- a week, six above it.
    bond_weeks          NUMERIC(3,1),

    bills_included      VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    bills_note          VARCHAR(300),

    available_from      DATE,
    min_term_months     INTEGER,
    furnished           BOOLEAN,

    -- The number of rooms let in the property, which decides the tenancy's
    -- legal character: two or more makes it a rooming house under Part 7,
    -- one leaves the occupant outside the Act entirely. Rarely stated in an
    -- advertisement, so null is the common case and means unknown rather
    -- than none.
    rooms_let           INTEGER,

    gender_preference   VARCHAR(20),
    couples_allowed     BOOLEAN,
    pets_allowed        BOOLEAN,
    smoking_allowed     BOOLEAN,

    inspection_note     VARCHAR(300),

    -- NONE: facts only, no contact, no photographs.
    -- LINK_ONLY: as above, and the original advertisement is the way to make
    --   contact.
    -- FULL: the advertiser has given permission for their details and images.
    consent_status      VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    consent_note        TEXT,
    source_url          VARCHAR(500),

    -- Only permitted where consent_status = 'FULL'. Enforced in the service
    -- as well as here, because a check constraint reports a violation
    -- without saying which field caused it.
    contact             VARCHAR(300),

    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',

    -- A listing for a property already let costs a reader a message and DAK
    -- its credibility. Expiry is set on publication rather than left to
    -- someone remembering.
    expires_at          TIMESTAMPTZ,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_rentals_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_rentals_listing_type
        CHECK (listing_type IN ('SHARE_ROOM', 'WHOLE_PROPERTY',
                                'LEASE_TRANSFER', 'STUDENT_ACCOMMODATION')),
    CONSTRAINT ck_rentals_bills
        CHECK (bills_included IN ('INCLUDED', 'EXCLUDED', 'OPTIONAL', 'UNKNOWN')),
    CONSTRAINT ck_rentals_consent
        CHECK (consent_status IN ('NONE', 'LINK_ONLY', 'FULL')),
    CONSTRAINT ck_rentals_rent_range
        CHECK (rent_max IS NULL OR rent_max >= rent_min),
    -- The rule the whole table is built around, in the one place it cannot
    -- be forgotten.
    CONSTRAINT ck_rentals_contact_requires_consent
        CHECK (contact IS NULL OR consent_status = 'FULL')
);

CREATE UNIQUE INDEX ux_rentals_slug ON rentals (slug);
CREATE INDEX ix_rentals_status_expires ON rentals (status, expires_at);
CREATE INDEX ix_rentals_suburb ON rentals (suburb);

-- Photographs, in display order. Separate table matching event_images, so the
-- existing Gallery component works without change.
CREATE TABLE rental_images (
    id            UUID PRIMARY KEY,
    rental_id     UUID NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    image_url     VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(300),
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX ix_rental_images_rental ON rental_images (rental_id, display_order);