-- Email verification tokens.
--
-- Same shape as password reset tokens and deliberately a separate table: the
-- two have different lifetimes, are spent under different circumstances, and
-- holding one should never imply anything about the other. A single table with
-- a type column would make every query say which kind it meant.
--
-- Longer-lived than a reset token. A reset is something someone is waiting for;
-- a verification email may sit unread for a day before anyone notices it, and
-- an expired link there costs a resend rather than an account.
CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_email_verification_tokens_hash
    ON email_verification_tokens (token_hash);

CREATE INDEX idx_email_verification_tokens_user
    ON email_verification_tokens (user_id);