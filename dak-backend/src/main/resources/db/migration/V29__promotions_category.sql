-- A category for offers rather than gatherings.
--
-- Promotions reuse the events table because what they need from it is what
-- events already have: a window with an end, a listing that drops out when
-- that end passes, and a detail page that survives afterwards. What differs
-- is presentation - a voucher has no time of day, no venue, and nothing to
-- put in a diary - and that is handled by three conditionals keyed on this
-- slug rather than by a second content type.
--
-- The operating rule is that only offers worth a reader's attention go in:
-- a long window, conditions that are easy to miss, and a real amount of
-- money. A weekly supermarket catalogue has none of those and will not be
-- listed. What DAK adds is the fine print, not the price.

INSERT INTO event_categories (name, slug) VALUES
    ('할인·프로모션', 'promotions');