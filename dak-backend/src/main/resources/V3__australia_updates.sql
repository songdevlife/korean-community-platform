-- V3__australia_updates.sql
-- Australia Updates core: categories, sources, updates, source references.
-- Deferred to a later migration: the AI URL-import workflow (05 API Spec §10.5
-- POST /api/v1/admin/australia-updates/import) — this migration only supports
-- manually-authored updates by an administrator.

-- 1. Update Categories -----------------------------------------------------
CREATE TABLE update_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_update_categories_name UNIQUE (name),
    CONSTRAINT uq_update_categories_slug UNIQUE (slug)
);

-- 2. Update Sources (publisher organisations) --------------------------------
CREATE TABLE update_sources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_update_sources_name UNIQUE (name),
    CONSTRAINT ck_update_sources_type CHECK (source_type IN (
        'OFFICIAL_GOVERNMENT', 'OFFICIAL_ORGANISATION', 'LOCAL_AUTHORITY',
        'NEWS_MEDIA', 'COMMUNITY_ORGANISATION', 'SOCIAL_MEDIA', 'USER_SUBMISSION', 'OTHER'
    ))
);

-- 3. Australia Updates ------------------------------------------------------
-- Column set per 04 Database Design §8.5.1 / §12.5 (title, Korean summary,
-- category, geographic scope, status).
CREATE TABLE australia_updates (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(300) NOT NULL,
    korean_summary    TEXT NOT NULL,
    category_id       UUID NOT NULL,
    geographic_scope  VARCHAR(50) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ai_generated      BOOLEAN NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_australia_updates_category_id FOREIGN KEY (category_id)
        REFERENCES update_categories (id),
    CONSTRAINT ck_australia_updates_status
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_australia_updates_scope CHECK (geographic_scope IN (
        'ADELAIDE', 'SOUTH_AUSTRALIA', 'AUSTRALIA', 'COUNCIL_AREA', 'SUBURB'
    ))
);

CREATE INDEX idx_australia_updates_status ON australia_updates (status);
CREATE INDEX idx_australia_updates_category ON australia_updates (category_id);

-- 4. Update <-> Source Reference junction table ---------------------------
-- Per 04 Database Design §8.5.3 — one update can cite multiple sources.
CREATE TABLE update_source_references (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    australia_update_id UUID NOT NULL,
    source_id           UUID NOT NULL,
    source_url          TEXT NOT NULL,
    source_title        VARCHAR(300),
    accessed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_usr_update_id FOREIGN KEY (australia_update_id)
        REFERENCES australia_updates (id) ON DELETE CASCADE,
    CONSTRAINT fk_usr_source_id FOREIGN KEY (source_id)
        REFERENCES update_sources (id),
    CONSTRAINT uq_usr_update_source_url UNIQUE (australia_update_id, source_url)
);

-- 5. Seed data --------------------------------------------------------------
INSERT INTO update_categories (name, slug) VALUES
    ('Immigration', 'immigration'),
    ('Government', 'government'),
    ('Transport', 'transport'),
    ('Healthcare', 'healthcare'),
    ('Cost of Living', 'cost-of-living'),
    ('Weather', 'weather'),
    ('Community', 'community');