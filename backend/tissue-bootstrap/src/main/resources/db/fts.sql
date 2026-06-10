-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- Issue full-text search schema (PostgreSQL)
--
-- Applied in TESTS via @Sql(BEFORE_TEST_CLASS) on IssueFullTextSearchIntegrationTest (runs after
-- Hibernate's `ddl-auto: create` builds the schema, replacing the plain `search_vector` column
-- Hibernate emits from the entity mapping with a GENERATED one + GIN index). PRODUCTION owns this
-- DDL out-of-band (no migration tool yet) — apply the same statements manually until Flyway.
--
-- The 'simple' text-search config MUST match IssueFtsFunctionContributor (plainto_tsquery('simple', ...)).

ALTER TABLE issue DROP COLUMN IF EXISTS search_vector;
ALTER TABLE issue ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(issue_key, '') || ' ' || coalesce(title, '') || ' ' || coalesce(content, ''))) STORED;
CREATE INDEX IF NOT EXISTS idx_issue_search_vector ON issue USING gin (search_vector);
