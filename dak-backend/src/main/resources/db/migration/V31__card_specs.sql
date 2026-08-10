-- The card text and layout decisions, stored so they stop changing.
--
-- The model rewords its output on every call. That meant a restart produced a
-- different visual description, the hash identifying the stored artwork no
-- longer matched, and new artwork was generated for a story that had not
-- changed. It also meant the card an administrator approved was not
-- necessarily the card that would be rendered next.
--
-- Held as JSON rather than as columns because the shape has changed three
-- times already — headerTitle, layoutType and infoBlocks were each added
-- after the first version — and none of it is queried.

CREATE TABLE card_specs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    content_type  VARCHAR(32) NOT NULL,
    content_id    UUID        NOT NULL,

    spec_json     JSONB       NOT NULL,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_card_specs_content_type
        CHECK (content_type IN ('AU_UPDATE', 'GUIDE'))
);

-- One spec per piece of content. Regenerating replaces it rather than adding
-- to it: an earlier spec is a draft of this one, not a separate artefact.
CREATE UNIQUE INDEX ux_card_specs_content
    ON card_specs (content_type, content_id);