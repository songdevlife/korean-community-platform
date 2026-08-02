-- Events.
--
-- Filling the gap the business directory cannot yet fill: a directory with no
-- approved listings reads as an unfinished site, while a handful of real
-- events reads as a working one. The same substitution AU Updates and guides
-- already made — content the owner can produce alone, rather than content that
-- needs other people to arrive first.
CREATE TABLE event_categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL,
    slug       VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_event_categories_slug ON event_categories (slug);

-- Chosen against a thousand posts from the incumbent Korean Adelaide board
-- rather than from guesswork. Sports clubs dominate there by a wide margin,
-- and language exchange barely appears — but the audience DAK is also aiming
-- at, working-holiday and student arrivals on Facebook, is the reverse. Both
-- are represented; the ordering reflects the board, not the assumption.
INSERT INTO event_categories (name, slug) VALUES
    ('스포츠·동호회',      'sports'),
    ('공연·문화',          'arts'),
    ('파티·모임·네트워킹', 'social'),
    ('봉사·나눔',          'volunteering'),
    ('언어교환',           'language-exchange'),
    ('무료강좌',           'classes');

CREATE TABLE events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(300) NOT NULL,
    description   TEXT,

    -- Required, and the reason this table needs its own status handling: an
    -- event that has happened is not merely old, it is wrong to show. Listings
    -- filter on starts_at rather than deleting the row, so a past event stays
    -- recoverable and stays available at its own URL.
    starts_at     TIMESTAMPTZ NOT NULL,
    ends_at       TIMESTAMPTZ,

    -- Free text rather than a foreign key to a venue table. Events happen in
    -- pubs, parks, church halls and people's houses, and normalising that
    -- would mean a venue record for every one-off.
    venue_name    VARCHAR(200),
    venue_address VARCHAR(300),

    -- Null means unknown rather than free; is_free carries that separately, so
    -- a listing can say "무료" with confidence rather than by inference from a
    -- missing number.
    is_free       BOOLEAN NOT NULL DEFAULT false,
    price_note    VARCHAR(100),

    organiser     VARCHAR(200),

    -- The organiser's own public contact, copied from a post they made to be
    -- found by. Distinct from a rental listing's phone number, which exists to
    -- reach one counterparty rather than an audience — but it is still a third
    -- party's personal information sitting on DAK, so it goes in only where
    -- the organiser has been asked, and comes out the moment they ask.
    -- Prefer an account, page or open-chat link over a personal number.
    organiser_contact VARCHAR(300),

    -- Where this was found. Facebook and Instagram publish no feed and block
    -- scraping, so every event here is entered by hand from a post someone
    -- read — and the link is what lets a reader check the original and reach
    -- the organiser, neither of which DAK can do for them.
    source_url    VARCHAR(500),

    category_id   UUID REFERENCES event_categories(id),

    -- DRAFT until reviewed, as with Australia Updates. Unnecessary while the
    -- owner is the only author; the point is that opening submissions later
    -- needs no change to how publication works.
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

-- The listing query is always "published, upcoming, soonest first".
CREATE INDEX idx_events_status_starts_at ON events (status, starts_at);
CREATE INDEX idx_events_category ON events (category_id);