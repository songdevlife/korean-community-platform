-- What people look for, with no record of who looked.
-- Deliberately carries no user id and no IP address: the question this answers
-- is "what does the audience want", not "what did this person do". Keeping it
-- anonymous also keeps it out of scope as personal information.
CREATE TABLE search_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_term   VARCHAR(200) NOT NULL,
    result_count  INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trend queries read by date; term queries group by the phrase. The zero-result
-- rows are the ones worth reading, so result_count is indexed alongside.
CREATE INDEX idx_search_logs_created_at ON search_logs(created_at DESC);
CREATE INDEX idx_search_logs_term ON search_logs(search_term);
CREATE INDEX idx_search_logs_result_count ON search_logs(result_count);