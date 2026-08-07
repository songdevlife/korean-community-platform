-- Australia Updates addressed by slug rather than by UUID.
--
-- Backfilled with the creation date and a fragment of the id, as V25 did for
-- events. Korean headlines slugify to nothing, so there is no title to derive
-- these from retrospectively; new rows get an English slug from the summariser
-- instead, and these can be rewritten one at a time from the admin queue.
--
-- Old UUID URLs keep working because the lookup accepts either form.

ALTER TABLE australia_updates ADD COLUMN slug VARCHAR(320);

UPDATE australia_updates
SET slug = 'update-'
        || to_char(created_at AT TIME ZONE 'Australia/Adelaide', 'YYYY-MM-DD')
        || '-'
        || substring(id::text, 1, 8);

ALTER TABLE australia_updates ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX ux_australia_updates_slug ON australia_updates (slug);