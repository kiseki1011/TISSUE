-- ============================================
-- LLM GENERATED
-- model: claude-opus-4-8
-- evaluation: NOT_REVIEWED
-- ============================================
-- pg_trgm trigram GIN index for fuzzy / substring search on wiki tag names.
--
-- Backs WikiTagRepository.findByName_NormalizedNameContaining (LIKE '%x%'), which the btree
-- unique constraint (uk_wiki_tag_normalized_name) cannot accelerate because of the leading
-- wildcard — without this index it is a full table scan. The GIN trigram index also enables
-- similarity() / `%` ranked, typo-tolerant matching if we want it later.
--
-- Applied out-of-band: test @Sql(BEFORE_TEST_CLASS) on WikiTagServiceIntegrationTest;
-- PRODUCTION applies manually until Flyway.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_wiki_tag_normalized_name_trgm ON wiki_tag USING gin (normalized_name gin_trgm_ops);
