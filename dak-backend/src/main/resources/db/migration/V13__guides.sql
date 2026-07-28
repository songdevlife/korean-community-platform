CREATE TABLE guide_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE guides (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(300) NOT NULL,
    slug          VARCHAR(320) NOT NULL UNIQUE,
    summary       VARCHAR(500),
    body          TEXT NOT NULL,
    category_id   UUID REFERENCES guide_categories(id),
    author_id     UUID REFERENCES users(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_guides_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_guides_status ON guides(status);
CREATE INDEX idx_guides_category ON guides(category_id);
CREATE INDEX idx_guides_published_at ON guides(published_at DESC);

INSERT INTO guide_categories (name, slug) VALUES
    ('Getting Started',  'getting-started'),
    ('Visa & Residency', 'visa-residency'),
    ('Study',            'study'),
    ('Housing',          'housing'),
    ('Healthcare',       'healthcare'),
    ('Transport',        'transport'),
    ('Money & Banking',  'money-banking'),
    ('Daily Life',       'daily-life');