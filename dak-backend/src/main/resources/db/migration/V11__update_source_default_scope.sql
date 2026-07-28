-- Imported updates arrive with no geographic scope, which is one of three
-- fields required before publication. Scope is a property of the source far
-- more often than of the individual article: an Adelaide feed produces
-- Adelaide news. Storing a default per source lets the import fill it in,
-- leaving the admin only the category to decide.
--
-- Nullable: a general national feed may have no sensible default, and an
-- admin can still override the value on any individual update.

ALTER TABLE update_sources
    ADD COLUMN default_geographic_scope VARCHAR(50);

ALTER TABLE update_sources
    ADD CONSTRAINT ck_update_sources_default_scope
    CHECK (default_geographic_scope IS NULL OR default_geographic_scope IN (
        'ADELAIDE', 'SOUTH_AUSTRALIA', 'AUSTRALIA', 'COUNCIL_AREA', 'SUBURB'
    ));

-- Existing sources. ABC's general national feed is scoped nationally.
UPDATE update_sources
SET default_geographic_scope = 'AUSTRALIA'
WHERE name = 'ABC News Australia';

UPDATE update_sources
SET default_geographic_scope = 'AUSTRALIA'
WHERE name = 'Department of Home Affairs';