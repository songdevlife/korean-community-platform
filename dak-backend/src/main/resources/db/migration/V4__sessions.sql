-- V4__sessions.sql
-- Session table for revocable refresh tokens (05 API Spec §3.4/§3.3/§12.1).
-- Access tokens remain self-contained JWTs (short-lived, no DB lookup needed).
-- Refresh tokens are now opaque random strings; only their SHA-256 hash is stored here,
-- never the raw token (05 API Spec §3.4 "Raw refresh tokens should not be stored in plain text").

CREATE TABLE sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    revoked            BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at         TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_sessions_user_id FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_sessions_refresh_token_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_sessions_user_id ON sessions (user_id);