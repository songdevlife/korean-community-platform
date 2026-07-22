-- V6__rss_feed_sources.sql
-- Adds RSS polling support to update_sources (04 Database Design §8.5.2 lists
-- "RSS feeds and automated ingestion" as a future enhancement — this is that phase).

ALTER TABLE update_sources ADD COLUMN rss_feed_url TEXT;
ALTER TABLE update_sources ADD COLUMN last_polled_at TIMESTAMPTZ;

-- Duplicate detection per 04 Database Design §12.5 ("Duplicate Detection... using
-- combinations of Normalised source URL"): each imported article's origin URL is
-- tracked so the poller can skip articles it has already imported.
ALTER TABLE update_source_references ADD COLUMN IF NOT EXISTS imported_via_poll BOOLEAN NOT NULL DEFAULT FALSE;