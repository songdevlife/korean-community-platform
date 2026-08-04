-- Poster image for an event.
--
-- A URL rather than a file. DAK has no upload endpoint and no object storage;
-- images are put on Cloudinary by hand and only the address is stored, which
-- is what the business images table already does. When submissions open to
-- users this becomes an upload path and probably a different provider, and
-- the column survives either way because what it holds is an address.
--
-- Nullable, and expected to be null more often than not: a council library
-- programme has no poster, and inventing one would be worse than showing none.
ALTER TABLE events ADD COLUMN image_url VARCHAR(500);

COMMENT ON COLUMN events.image_url IS
    'Absolute URL of a poster image, used only where the organiser has given permission. Null where there is no image or no permission.';