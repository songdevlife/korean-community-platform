-- V7__community.sql
-- Community core: posts + comments (self-referencing for replies).
-- Deferred to a later migration: post_reactions, content_reports.

CREATE TABLE community_posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID NOT NULL,
    category    VARCHAR(30) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,

    CONSTRAINT fk_community_posts_author_id FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT ck_community_posts_category CHECK (category IN
        ('GENERAL', 'QUESTIONS', 'BUY_AND_SELL', 'JOBS', 'EVENTS')),
    CONSTRAINT ck_community_posts_status CHECK (status IN ('PUBLISHED', 'HIDDEN', 'DELETED'))
);

CREATE INDEX idx_community_posts_author ON community_posts (author_id);
CREATE INDEX idx_community_posts_status_created ON community_posts (status, created_at);

CREATE TABLE community_comments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id           UUID NOT NULL,
    author_id         UUID NOT NULL,
    parent_comment_id UUID,
    content           TEXT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_community_comments_post_id FOREIGN KEY (post_id)
        REFERENCES community_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_author_id FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_community_comments_parent_id FOREIGN KEY (parent_comment_id)
        REFERENCES community_comments (id) ON DELETE CASCADE,
    CONSTRAINT ck_community_comments_status CHECK (status IN ('PUBLISHED', 'HIDDEN', 'DELETED'))
);

CREATE INDEX idx_community_comments_post_created ON community_comments (post_id, created_at);
CREATE INDEX idx_community_comments_parent ON community_comments (parent_comment_id);