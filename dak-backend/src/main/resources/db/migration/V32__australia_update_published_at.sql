-- When an update went public, as distinct from when it was imported.
--
-- The two are not the same. An article is fetched, sits in the queue until an
-- administrator has written a Korean summary for it, and is published some
-- days later. Ordering by created_at puts that update below one imported
-- afterwards and published sooner, so the newest thing on the site is not the
-- first thing a reader sees.

ALTER TABLE australia_updates
    ADD COLUMN published_at TIMESTAMPTZ;

-- Existing published rows have no record of when they went out. Their import
-- time is the closest thing available and preserves their relative order.
UPDATE australia_updates
SET published_at = created_at
WHERE status = 'PUBLISHED';

CREATE INDEX idx_australia_updates_published_at
    ON australia_updates (published_at DESC);