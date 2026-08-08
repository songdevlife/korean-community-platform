-- When a listing DAK did not receive from the advertiser was last verified.
--
-- Distinct from updated_at, which moves when a typo is corrected and says
-- nothing about whether the room is still available. content-policy.md
-- section 18 undertakes to display this date on every external listing, and
-- an undertaking needs a field rather than an inference.
--
-- Null for consented listings, where the advertiser is reachable and the
-- question does not arise.

ALTER TABLE rentals ADD COLUMN last_checked_at TIMESTAMPTZ;