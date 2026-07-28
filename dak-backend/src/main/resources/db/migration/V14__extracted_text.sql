-- Separates machine-extracted source text from the human-written Korean summary.
--
-- Both previously shared korean_summary, so an admin who skipped the rewrite
-- published the original article's text verbatim. Extraction now lands in its
-- own column, which is exposed to administrators only, and korean_summary holds
-- nothing but what a person wrote.
ALTER TABLE australia_updates
    ADD COLUMN extracted_text TEXT;

-- Imports arrive with no summary, so the column must permit null. Publication
-- is gated in the service layer instead.
ALTER TABLE australia_updates
    ALTER COLUMN korean_summary DROP NOT NULL;

-- Existing AI drafts hold extracted text in the wrong column. Move it rather
-- than discarding it — it is still the raw material for a rewrite — and clear
-- the summary so nothing publishes without review.
UPDATE australia_updates
SET extracted_text = korean_summary,
    korean_summary = NULL
WHERE ai_generated = true
  AND status <> 'PUBLISHED';