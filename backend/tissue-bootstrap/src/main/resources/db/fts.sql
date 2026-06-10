-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- Full-text search schema (PostgreSQL)
-- Issue + WikiDocument tsvector columns + GIN indexes
--
-- Applied in the FTS integration tests via @Sql(BEFORE_TEST_CLASS).
-- (IssueFullTextSearchIntegrationTest, WikiQueryServiceIntegrationTest)
-- Runs after Hibernate's `ddl-auto: create` builds the schema, replacing the plain `search_vector` columns.
-- Hibernate emits from the entity mappings with generated ones + GIN indexes.
-- Production owns this DDL.
--
-- The 'simple' text-search config must match IssueFtsFunctionContributor (plainto_tsquery('simple', ...)).

-- Issue: issue_key + title + content
ALTER TABLE issue DROP COLUMN IF EXISTS search_vector;
ALTER TABLE issue ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(issue_key, '') || ' ' || coalesce(title, '') || ' ' || coalesce(content, ''))) STORED;
CREATE INDEX IF NOT EXISTS idx_issue_search_vector ON issue USING gin (search_vector);

-- WikiDocument: title + content
ALTER TABLE wiki_document DROP COLUMN IF EXISTS search_vector;
ALTER TABLE wiki_document ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))) STORED;
CREATE INDEX IF NOT EXISTS idx_wiki_document_search_vector ON wiki_document USING gin (search_vector);
