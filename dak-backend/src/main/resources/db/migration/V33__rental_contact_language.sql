-- Which language the person advertising the listing can be contacted in.
-- Korean readers need this before they write, not after a message goes
-- unanswered. UNKNOWN is the default and is not displayed: an external
-- listing has never been spoken to, and guessing here would produce a
-- badge that discourages contact on no evidence.
ALTER TABLE rentals
    ADD COLUMN contact_language VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE rentals
    ADD CONSTRAINT ck_rentals_contact_language
        CHECK (contact_language IN ('KOREAN', 'ENGLISH', 'BOTH', 'UNKNOWN'));