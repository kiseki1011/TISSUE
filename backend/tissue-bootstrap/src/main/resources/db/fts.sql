-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- Full-text search schema (PostgreSQL)
-- Issue + WikiDocument tsvector columns + GIN indexes
--
-- Production owns this DDL.
--
-- Config = 'simple' (lowercase + tokenize only)
-- Must be the same in all 3 spots, or indexed vs. queried lexemes won't align and matching silently breaks.
--   1) to_tsvector('simple', ...) on issue.search_vector         in fts.sql (this file)
--   2) to_tsvector('simple', ...) on wiki_document.search_vector in fts.sql (this file)
--   3) to_tsquery('simple', ...)                                 in IssueFtsFunctionContributor
--
-- If content is guaranteed English, you can change 'simple' to 'english'.

-- Issue: issue_key + title + content
ALTER TABLE issue DROP COLUMN IF EXISTS search_vector;
ALTER TABLE issue ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(issue_key, '') || ' ' || coalesce(title, '') || ' ' || coalesce(content, ''))) STORED;
CREATE INDEX IF NOT EXISTS idx_issue_search_vector ON issue USING gin (search_vector);

-- WikiDocument: title + content
ALTER TABLE wiki_document DROP COLUMN IF EXISTS search_vector;
ALTER TABLE wiki_document ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))) STORED;
CREATE INDEX IF NOT EXISTS idx_wiki_document_search_vector ON wiki_document USING gin (search_vector);
