-- V8__saved_items.sql
-- Saved Items (05 API Spec §4.3): lets a user bookmark businesses, guides,
-- community posts, or Australia Updates for later. Uses a resource-type + resource-id
-- pair (per 04 Database Design §8.12.1) rather than separate tables per resource type,
-- since this is a simple "did I save this" relationship with no extra metadata.

CREATE TABLE saved_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id   UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_saved_items_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_saved_items_resource_type CHECK (resource_type IN
        ('BUSINESS', 'GUIDE', 'COMMUNITY_POST', 'AUSTRALIA_UPDATE')),
    CONSTRAINT uq_saved_items_user_resource UNIQUE (user_id, resource_type, resource_id)
);

CREATE INDEX idx_saved_items_user ON saved_items (user_id, created_at);