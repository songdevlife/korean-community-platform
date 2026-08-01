-- Display names must be distinct, case-insensitively.
--
-- The service checks before writing, which covers the ordinary case and gives
-- a usable message. It cannot cover two requests arriving together, so the
-- guarantee lives here. Functional index on lower() rather than a plain unique
-- constraint, because two names differing only in case read as the same name
-- to a person and allowing both would make impersonation trivial.
CREATE UNIQUE INDEX uq_users_display_name_lower
    ON users (lower(display_name));