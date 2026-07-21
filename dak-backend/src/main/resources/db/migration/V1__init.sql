-- V1__init.sql
-- Backend Step 2, migration 1 of the sequence defined in 04_Database_Design_DAK.docx §15.11:
--   1. Extensions and database configuration   <- this file
--   2. Geographic reference tables              <- later (V2+)
--   3. User and role tables                     <- this file
-- Scope for this first migration: just enough for authentication (users, roles, user_roles).
-- Everything else (businesses, community, etc.) follows in later Vn files.

-- 1. Extensions -------------------------------------------------------------
-- gen_random_uuid() lives in pgcrypto on older PG, and is core in PG13+, but enabling
-- pgcrypto explicitly keeps this portable and matches 04 Database Design's use of UUID PKs.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Roles --------------------------------------------------------------------
-- Physical table name and purpose per 04 Database Design §8.1.2.
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- 3. Users ----------------------------------------------------------------
-- Column set per 04 Database Design §8.1.1, §12.2 (email/password/status rules)
-- and §10.10 constraint summary matrix.
CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    display_name      VARCHAR(100) NOT NULL,
    profile_image_url TEXT,
    email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    account_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_account_status
        CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED', 'DELETED'))
);

-- Case-insensitive email lookups/uniqueness, per 04 Database Design §11.4.
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

-- 4. User <-> Role junction table --------------------------------------------
-- Per 04 Database Design §8.1.3 and the composite-unique rule in §10.10.
CREATE TABLE user_roles (
    user_id    UUID NOT NULL,
    role_id    UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id_users FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id_roles FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_user ON user_roles (role_id, user_id);

-- 5. Seed data ----------------------------------------------------------------
-- Per 04 Database Design §15.12: roles need stable seed data since application logic
-- (e.g. default role on signup) depends on these records existing.
INSERT INTO roles (name, description) VALUES
    ('USER', 'Standard registered user'),
    ('BUSINESS_OWNER', 'Manages one or more business listings'),
    ('MODERATOR', 'Reviews and moderates community content'),
    ('ADMINISTRATOR', 'Full platform administrative access');
