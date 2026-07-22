-- V2__business_directory.sql
-- Business Directory core: categories, businesses, category assignments.
-- Deferred to later migrations: operating hours, reviews, business managers,
-- verification records (each has its own table per 04 Database Design §8.2.4-8.2.7).

-- 1. Business Categories ------------------------------------------------------
CREATE TABLE business_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_business_categories_name UNIQUE (name),
    CONSTRAINT uq_business_categories_slug UNIQUE (slug)
);

-- 2. Businesses ----------------------------------------------------------------
-- Column set per 04 Database Design §8.2.1 / §12.3 (name, slug, contact, address, status rules).
CREATE TABLE businesses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    slug              VARCHAR(220) NOT NULL,
    short_description VARCHAR(300),
    description       TEXT,

    phone             VARCHAR(30),
    email             VARCHAR(255),
    website_url       TEXT,

    address_line      VARCHAR(255),
    suburb            VARCHAR(100),
    state             VARCHAR(10),
    postcode          VARCHAR(4),
    country           VARCHAR(2) NOT NULL DEFAULT 'AU',
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,

    korean_available  VARCHAR(40) NOT NULL DEFAULT 'UNVERIFIED',
    verified          BOOLEAN NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT uq_businesses_slug UNIQUE (slug),
    CONSTRAINT ck_businesses_status
        CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT ck_businesses_korean_available CHECK (korean_available IN (
        'KOREAN_SPEAKING_OWNER', 'KOREAN_SPEAKING_STAFF', 'BY_APPOINTMENT',
        'TRANSLATION_ASSISTANCE', 'UNVERIFIED'
    )),
    CONSTRAINT ck_businesses_latitude CHECK (latitude IS NULL OR (latitude BETWEEN -90 AND 90)),
    CONSTRAINT ck_businesses_longitude CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180))
);

CREATE INDEX idx_businesses_suburb ON businesses (suburb);
CREATE INDEX idx_businesses_status ON businesses (status);

-- 3. Business <-> Category junction table --------------------------------
CREATE TABLE business_category_assignments (
    business_id  UUID NOT NULL,
    category_id  UUID NOT NULL,
    is_primary   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_business_category_assignments PRIMARY KEY (business_id, category_id),
    CONSTRAINT fk_bca_business_id FOREIGN KEY (business_id)
        REFERENCES businesses (id) ON DELETE CASCADE,
    CONSTRAINT fk_bca_category_id FOREIGN KEY (category_id)
        REFERENCES business_categories (id) ON DELETE CASCADE
);

CREATE INDEX idx_bca_category_business ON business_category_assignments (category_id, business_id);

-- 4. Seed data ------------------------------------------------------------
-- Per 03 MVP Feature Specification §6 example categories.
INSERT INTO business_categories (name, slug) VALUES
    ('Restaurants', 'restaurants'),
    ('Cafes', 'cafes'),
    ('Grocery Stores', 'grocery-stores'),
    ('Beauty', 'beauty'),
    ('Healthcare', 'healthcare'),
    ('Education', 'education'),
    ('Immigration Services', 'immigration-services');