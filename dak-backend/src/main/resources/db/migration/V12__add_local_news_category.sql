-- The seeded categories cover service and government information, but the
-- Adelaide feed also carries general local news — court proceedings, council
-- decisions, incidents — which had nowhere to go. Without a category an
-- update cannot be published, so those items could only be archived.
--
-- Deliberately one broad category rather than several narrow ones: the mix of
-- local stories that actually arrives is not yet known, and splitting it later
-- against real data is easier than guessing now and leaving categories empty.

INSERT INTO update_categories (id, name, slug)
VALUES (gen_random_uuid(), 'Local News', 'local-news');