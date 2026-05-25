-- ============================================================
-- Load test admin account
-- Creates a real login capable admin and adds it to seeded WS0001
-- as OWNER + MANAGER of every project in that workspace.
--
-- Credentials:
--   email    = loadadmin@loadtest.local
--   password = Loadtest1!
--   (bcrypt hash generated via httpd htpasswd, strength=10)
--
-- Run AFTER seed.sql.
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- Member
INSERT INTO member (email, username, name, language, system_role, member_status)
VALUES ('loadadmin@loadtest.local', 'loadadmin', 'Load Admin', 'EN', 'USER', 'ACTIVE')
ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name
RETURNING id \gset adm_

-- AuthIdentity: insert BOTH provider rows so login works regardless of
-- the active profile's tissue.security.email-required setting.
--   - prod (email-required=true)  → MemberDetailsService looks up by EMAIL provider
--   - loadtest (email-required=false) → looks up by USERNAME provider
-- credential hash is bcrypt of "Loadtest1!".
INSERT INTO auth_identity (member_id, provider, identifier, credential)
VALUES
    (:adm_id, 'EMAIL',    'loadadmin@loadtest.local',
        '$2a$10$mFlYs59/FsJN34kmFRjdWeSlho.eFuLtOlPebGHcnczSnt48d3A4C'),
    (:adm_id, 'USERNAME', 'loadadmin@loadtest.local',
        '$2a$10$mFlYs59/FsJN34kmFRjdWeSlho.eFuLtOlPebGHcnczSnt48d3A4C')
ON CONFLICT (provider, identifier) DO UPDATE SET credential = EXCLUDED.credential;

-- WorkspaceMember (OWNER of WS0001)
INSERT INTO workspace_member (
    workspace_id, member_id, workspace_key, workspace_role,
    archived, soft_deleted
)
SELECT w.id, :adm_id, w.workspace_key, 'OWNER', false, false
FROM workspace w
WHERE w.workspace_key = 'WS0001'
RETURNING id \gset wm_

-- ProjectMember (MANAGER on every project in WS0001)
INSERT INTO project_member (
    project_id, workspace_member_id, member_id,
    workspace_key, project_key, project_role,
    archived, soft_deleted
)
SELECT
    p.id,
    :wm_id,
    :adm_id,
    p.workspace_key,
    p.project_key,
    'MANAGER',
    false, false
FROM project p
WHERE p.workspace_key = 'WS0001';

COMMIT;

\echo '======================================================'
\echo 'Load admin ready.'
\echo '  email   : loadadmin@loadtest.local'
\echo '  password: Loadtest1!'
\echo '  joined  : WS0001 (OWNER) + every project in WS0001 (MANAGER)'
\echo '======================================================'

SELECT 'admin member_id'      AS field, :adm_id::text AS value
UNION ALL
SELECT 'admin workspace_member_id', :wm_id::text
UNION ALL
SELECT 'projects in WS0001', count(*)::text FROM project WHERE workspace_key = 'WS0001';
