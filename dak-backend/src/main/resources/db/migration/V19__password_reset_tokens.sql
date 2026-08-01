-- Password reset tokens.
--
-- Separate from sessions rather than a flag on it: a session lasts fourteen
-- days and is renewed, a reset token lasts thirty minutes and is destroyed on
-- first use. Sharing a table would mean every query on either having to say
-- which kind it meant.
--
-- The token is stored hashed, as refresh tokens are. Anyone reading this table
-- would otherwise hold a working password reset for every row in it, which is
-- worse than reading the password hashes beside them.
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Lookup is always by hash: the caller presents a token, and the row is found
-- by hashing it. Unique because two rows with the same hash would make that
-- lookup ambiguous.
CREATE UNIQUE INDEX uq_password_reset_tokens_hash
    ON password_reset_tokens (token_hash);

-- Used when issuing a token, to invalidate any outstanding ones for the same
-- person — requesting a second reset should make the first link stop working.
CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens (user_id);