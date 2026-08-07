-- Events addressed by slug rather than by UUID.
--
-- Backfilled with a date and a fragment of the id: unlovely, but unique
-- without inspecting titles, which are Korean and would slugify to nothing.
-- Old UUID URLs keep working because the lookup accepts either form, so
-- these can be rewritten one at a time from the admin screen without
-- breaking anything that has already been shared.

ALTER TABLE events ADD COLUMN slug VARCHAR(320);

UPDATE events
SET slug = 'event-'
        || to_char(starts_at AT TIME ZONE 'Australia/Adelaide', 'YYYY-MM-DD')
        || '-'
        || substring(id::text, 1, 8);

ALTER TABLE events ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX ux_events_slug ON events (slug);