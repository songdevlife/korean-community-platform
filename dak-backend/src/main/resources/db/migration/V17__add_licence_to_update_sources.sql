-- Licence metadata for update sources.
--
-- CC BY 4.0 requires three things when material is reused: credit, a link to
-- the licence, and an indication that changes were made. The first is already
-- served by the source name and article link. These two columns carry the
-- second; the third is a property of how DAK works rather than of any one
-- source, so it lives in the frontend as fixed text.
--
-- Nullable because most sources are not openly licensed. ABC News is ordinary
-- copyright material that DAK reports on rather than reuses, and a licence
-- line under those articles would be a false claim about the source.
ALTER TABLE update_sources
    ADD COLUMN licence_name VARCHAR(50),
    ADD COLUMN licence_url  VARCHAR(255);

COMMENT ON COLUMN update_sources.licence_name IS
    'Open licence the source publishes under, e.g. CC BY 4.0. Null when the source is not openly licensed.';
COMMENT ON COLUMN update_sources.licence_url IS
    'Canonical URL of the licence deed. Required by CC BY 4.0 when licence_name is set.';

-- ACCC Product Safety publishes under CC BY 4.0. The other government sources
-- are left null until each has been checked against its own copyright page --
-- guessing at a licence is worse than showing none.
UPDATE update_sources
SET licence_name = 'CC BY 4.0',
    licence_url  = 'https://creativecommons.org/licenses/by/4.0/'
WHERE name = 'ACCC Product Safety - Compulsory recalls';