// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
-- Loadtest cleanup: removes rows that k6 inserted during a baseline run.
--
-- Identification rule: created_by = loadadmin.member_id.
--   * seed.sql is plain SQL → JPA auditing doesn't fire → created_by IS NULL
--   * k6 → API → JPA auditing → created_by = loadadmin.id
-- This means cleanup never touches the seeded dataset.
--
-- Caveats:
--   * PATCH-only mutations (issue priority, story_point, subscriber toggle)
--     cannot be rolled back. They are random/idempotent so re-runs converge.
--   * Subscriber rows added by toggle are also wiped here (subscriber_id =
--     loadadmin's project_member_id).
--
-- Usage:
--   docker exec -i tissue-loadtest-db psql -U tissue -d tissue -f /seed/cleanup.sql
-- ============================================================

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- Resolve loadadmin's identifiers
WITH adm AS (
    SELECT id FROM member WHERE email = 'loadadmin@loadtest.local'
)
SELECT id AS loadadmin_member_id FROM adm \gset

\if :{?loadadmin_member_id}
    \echo 'loadadmin member_id =' :loadadmin_member_id
\else
    \echo 'ERROR: loadadmin not found — run loadadmin.sql first'
    \quit
\endif

-- Delete in dependency order. Most have FK to issue/wiki/comment with
-- soft cascade rules so explicit order keeps it predictable.

DELETE FROM comment        WHERE created_by = :loadadmin_member_id;
DELETE FROM wiki_document  WHERE created_by = :loadadmin_member_id;

-- Subscriber toggle: rows whose subscriber_id is a project_member backed by loadadmin
DELETE FROM issue_subscriber
WHERE subscriber_id IN (
    SELECT pm.id FROM project_member pm WHERE pm.member_id = :loadadmin_member_id
);

-- Issue last - cascades to activity_log / notification / issue_subscriber /
-- issue_reviewer / issue_tag / comment via FK ON DELETE CASCADE (if configured)
-- or leaves orphans (acceptable for loadtest).
DELETE FROM issue WHERE created_by = :loadadmin_member_id;

COMMIT;

\echo '======================================================'
\echo 'Cleanup done. Remaining loadadmin-owned rows (should be 0):'
\echo '======================================================'
SELECT 'comment'          AS table, count(*) FROM comment        WHERE created_by = :loadadmin_member_id
UNION ALL SELECT 'wiki_document',  count(*) FROM wiki_document  WHERE created_by = :loadadmin_member_id
UNION ALL SELECT 'issue',          count(*) FROM issue          WHERE created_by = :loadadmin_member_id;
