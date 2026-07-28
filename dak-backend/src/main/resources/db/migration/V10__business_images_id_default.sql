-- V9 created business_images without a default on the primary key, so any
-- insert that omits id fails. JPA supplies the value, which is why the API
-- path worked and the gap only surfaced when seeding via SQL — and every
-- other table in the schema defaults to gen_random_uuid().
ALTER TABLE business_images ALTER COLUMN id SET DEFAULT gen_random_uuid();