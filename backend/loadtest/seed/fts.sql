-- ============================================================
-- AI-GENERATED
-- model: claude-opus-4-7
-- NOT REVIEWED
-- ============================================================
-- PostgreSQL tsvector + GIN setup for issue full-text search.
--
-- Run AFTER seed.sql (or after the table exists). Idempotent.
--
--   docker exec -i tissue-loadtest-db psql -U tissue -d tissue -f /seed/fts.sql
--
-- Notes
--   * `simple` configuration: no stemming, no stop-words. Matches the seed
--     vocabulary (lowercase ASCII single words) exactly. For real content with
--     CJK or other languages, swap in pg_trgm or a custom config.
--   * Generated column: PG keeps search_vector in sync automatically on INSERT
--     and UPDATE. No trigger needed.
--   * GIN index: ~30-40% of table size, but the only structure that makes
--     tsquery @@ tsvector logarithmic on a 10M-row table.
--   * issue_key included so queries like "P0001-1234" can match by FTS too.
--     The 'simple' config splits on punctuation, so "P0001-1234" tokenizes
--     to {p0001, 1234} — searchable as `plainto_tsquery('simple','p0001')`.
-- ============================================================

\set ON_ERROR_STOP on
\timing on

-- Re-create the generated column if the expression changed
ALTER TABLE issue DROP COLUMN IF EXISTS search_vector;

ALTER TABLE issue
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('simple',
            coalesce(issue_key, '') || ' ' ||
            coalesce(title, '')     || ' ' ||
            coalesce(content, ''))
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_issue_search_vector
    ON issue USING GIN (search_vector);

ANALYZE issue;

\echo 'tsvector + GIN ready.'

SELECT
    pg_size_pretty(pg_relation_size('issue'))                 AS issue_size,
    pg_size_pretty(pg_relation_size('idx_issue_search_vector')) AS gin_index_size;
