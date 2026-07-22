-- V5__relax_australia_update_constraints.sql
-- Supports the AI import workflow (05 API Spec §10.5): a freshly-imported draft
-- has a title and a draft summary, but no category or geographic scope yet —
-- those are filled in by an administrator before the update can be published.

ALTER TABLE australia_updates ALTER COLUMN category_id DROP NOT NULL;
ALTER TABLE australia_updates ALTER COLUMN geographic_scope DROP NOT NULL;