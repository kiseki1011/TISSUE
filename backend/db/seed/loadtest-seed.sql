-- ============================================================
-- Tissue loadtest seed
--
-- Usage (defaults = 10k issues, smoke):
--   docker exec -i tissue-loadtest-db psql -U tissue -d tissue \
--     -v ws_count=10 -v members_per_ws=20 -v proj_per_ws=10 -v issues_per_proj=100 \
--     -f /seed/loadtest-seed.sql
--
-- Scales (Profile A: large org):
--   10k:    ws=10  members_per_ws=20  proj_per_ws=10  issues_per_proj=100
--   1M:     ws=100 members_per_ws=100 proj_per_ws=10  issues_per_proj=1000
--   10M:    ws=100 members_per_ws=100 proj_per_ws=10  issues_per_proj=10000
--
-- Notes
--   * Members are domain rows only; they have no auth_identity → cannot login.
--     For k6 auth, create test admin separately via API.
--   * One global system workflow is shared across all projects.
--   * issue.content/summary (Large Object) and custom_fields left NULL.
-- ============================================================

\set ON_ERROR_STOP on
\timing on

-- Default scale: 10k issues (override with -v)
\if :{?ws_count}        \else \set ws_count        10  \endif
\if :{?members_per_ws}  \else \set members_per_ws  20  \endif
\if :{?proj_per_ws}     \else \set proj_per_ws     10  \endif
\if :{?issues_per_proj} \else \set issues_per_proj 100 \endif

\echo '======================================================'
\echo 'Seeding with: ws=' :ws_count ' members_per_ws=' :members_per_ws ' proj_per_ws=' :proj_per_ws ' issues_per_proj=' :issues_per_proj
\echo '======================================================'

BEGIN;

-- ------------------------------------------------------------
-- 1. Global system workflow + 4 states (one INITIAL/ACTIVE/COMPLETED/ABORTED)
-- ------------------------------------------------------------
WITH wf AS (
    INSERT INTO workflow (
        color, description, display_name, normalized_name,
        project_key, workspace_key, system_provided, version
    )
    VALUES ('BLUE', 'Loadtest default workflow', 'Default', 'default',
            'SYSTEM', 'SYSTEM', true, 0)
    RETURNING id
), s_initial AS (
    INSERT INTO workflow_state (
        state_category, color, description, display_name, normalized_name,
        workflow_id, version
    )
    SELECT 'INITIAL', 'GRAY',  'Initial state',   'To Do',       'to_do',       id, 0 FROM wf
    RETURNING id, workflow_id
), s_active AS (
    INSERT INTO workflow_state (
        state_category, color, description, display_name, normalized_name,
        workflow_id, version
    )
    SELECT 'ACTIVE',  'BLUE',  'In progress',     'In Progress', 'in_progress', id, 0 FROM wf
    RETURNING id
), s_done AS (
    INSERT INTO workflow_state (
        state_category, color, description, display_name, normalized_name,
        workflow_id, version
    )
    SELECT 'COMPLETED','GREEN','Completed',       'Done',        'done',        id, 0 FROM wf
    RETURNING id
), s_aborted AS (
    INSERT INTO workflow_state (
        state_category, color, description, display_name, normalized_name,
        workflow_id, version
    )
    SELECT 'ABORTED', 'RED',   'Aborted',         'Cancelled',   'cancelled',   id, 0 FROM wf
    RETURNING id
)
UPDATE workflow SET initial_state_id = (SELECT id FROM s_initial)
WHERE id = (SELECT workflow_id FROM s_initial);

-- ------------------------------------------------------------
-- 2. Members (no auth_identity → cannot login)
-- ------------------------------------------------------------
INSERT INTO member (email, username, name, language, system_role, member_status)
SELECT
    'load' || i || '@loadtest.local',
    'load' || i,
    'Load User ' || i,
    'EN',
    'USER',
    'ACTIVE'
FROM generate_series(1, :ws_count * :members_per_ws) AS s(i);

-- ------------------------------------------------------------
-- 3. Workspaces
-- ------------------------------------------------------------
INSERT INTO workspace (workspace_key, name, description, archived, soft_deleted)
SELECT
    'WS' || lpad(i::text, 4, '0'),
    'Workspace ' || i,
    'Loadtest workspace ' || i,
    false,
    false
FROM generate_series(1, :ws_count) AS s(i);

-- Capture workspace ids
CREATE TEMP TABLE _ws AS
SELECT id AS workspace_id, workspace_key,
       row_number() OVER (ORDER BY id) AS ws_idx
FROM workspace
WHERE workspace_key LIKE 'WS%';

-- Capture member ids
CREATE TEMP TABLE _mem AS
SELECT id AS member_id, username,
       row_number() OVER (ORDER BY id) AS mem_idx
FROM member
WHERE email LIKE 'load%@loadtest.local';

-- ------------------------------------------------------------
-- 4. workspace_member (each member belongs to exactly one workspace)
--    Layout: members[(w-1)*M+1 .. w*M] → workspace w, first is OWNER, rest MEMBER
-- ------------------------------------------------------------
INSERT INTO workspace_member (
    workspace_id, member_id, workspace_key, workspace_role,
    archived, soft_deleted
)
SELECT
    w.workspace_id,
    m.member_id,
    w.workspace_key,
    CASE WHEN ((m.mem_idx - 1) % :members_per_ws) = 0 THEN 'OWNER' ELSE 'MEMBER' END,
    false,
    false
FROM _mem m
JOIN _ws w ON w.ws_idx = ((m.mem_idx - 1) / :members_per_ws) + 1;

-- ------------------------------------------------------------
-- 5. Projects (proj_per_ws each workspace)
-- ------------------------------------------------------------
INSERT INTO project (
    workspace_id, workspace_key, project_key, title, description,
    visibility, issue_number, sprint_number, archived, soft_deleted
)
SELECT
    w.workspace_id,
    w.workspace_key,
    'P' || lpad(p::text, 4, '0'),
    'Project ' || p || ' of ' || w.workspace_key,
    'Loadtest project',
    'PUBLIC',
    0, 0, false, false
FROM _ws w
CROSS JOIN generate_series(1, :proj_per_ws) AS p;

-- Capture project ids
CREATE TEMP TABLE _proj AS
SELECT p.id AS project_id, p.workspace_id, p.workspace_key, p.project_key,
       row_number() OVER (PARTITION BY p.workspace_id ORDER BY p.id) AS proj_idx_in_ws
FROM project p
JOIN _ws w ON w.workspace_id = p.workspace_id;

-- ------------------------------------------------------------
-- 6. project_member (every workspace_member joins every project of that workspace as MEMBER;
--    the OWNER becomes MANAGER)
-- ------------------------------------------------------------
INSERT INTO project_member (
    project_id, workspace_member_id, member_id,
    workspace_key, project_key, project_role,
    archived, soft_deleted
)
SELECT
    pr.project_id,
    wm.id,
    wm.member_id,
    wm.workspace_key,
    pr.project_key,
    CASE WHEN wm.workspace_role = 'OWNER' THEN 'MANAGER' ELSE 'MEMBER' END,
    false,
    false
FROM workspace_member wm
JOIN _proj pr ON pr.workspace_id = wm.workspace_id;

-- Capture project_member ids per project (for issue.assignee_id)
CREATE TEMP TABLE _pm AS
SELECT pm.id AS project_member_id, pm.project_id,
       row_number() OVER (PARTITION BY pm.project_id ORDER BY pm.id) AS pm_idx
FROM project_member pm;

-- TEMP tables get no stats / no indexes by default; add what the seed JOINs need.
CREATE INDEX ON _pm (project_id, pm_idx);
CREATE INDEX ON _proj (project_id);
ANALYZE _proj;
ANALYZE _pm;

-- ------------------------------------------------------------
-- 7. issue_type (one STANDARD type per project, all sharing the global workflow)
-- ------------------------------------------------------------
INSERT INTO issue_type (
    project_id, workflow_id,
    color, icon, hierarchy, description,
    display_name, normalized_name, system_provided, version
)
SELECT
    pr.project_id,
    (SELECT id FROM workflow LIMIT 1),
    'BLUE', 'CIRCLE_FILLED', 'STANDARD',
    'Default task',
    'Task', 'task',
    true, 0
FROM _proj pr;

-- Capture issue_type ids
CREATE TEMP TABLE _it AS
SELECT it.id AS issue_type_id, it.project_id
FROM issue_type it;
CREATE INDEX ON _it (project_id);
ANALYZE _it;

-- ------------------------------------------------------------
-- 8. Issues (mass insert — set-based, no per-row subqueries)
--    Per project: issues_per_proj issues, issue_key = "P<NNNN>-<n>"
--    Priority/state/assignee distributed via deterministic round-robin.
-- ------------------------------------------------------------

-- Pre-resolve the global workflow_state ids into psql vars so the INSERT
-- doesn't re-run a SELECT for every row.
SELECT id AS initial_state_id FROM workflow_state WHERE state_category = 'INITIAL' LIMIT 1 \gset

-- Each project has exactly `members_per_ws` project_members (every workspace_member
-- joins every project in that workspace). So assignee selection becomes a simple JOIN.

INSERT INTO issue (
    project_id, workspace_key, issue_type_id, current_state_id, assignee_id,
    issue_key, title, priority, story_point,
    count_based_progress, point_based_progress,
    archived, soft_deleted, version
)
SELECT
    pr.project_id,
    pr.workspace_key,
    it.issue_type_id,
    :initial_state_id,
    pm.project_member_id,
    pr.project_key || '-' || n,
    'Issue ' || n || ' for ' || pr.project_key,
    (ARRAY['P0','P1','P2','P3','P4'])[((n - 1) % 5) + 1],
    ((n - 1) % 13) + 1,
    0, 0, false, false, 0
FROM _proj pr
JOIN _it it ON it.project_id = pr.project_id
CROSS JOIN generate_series(1, :issues_per_proj) AS n
JOIN _pm pm ON pm.project_id = pr.project_id
            AND pm.pm_idx = ((n - 1) % :members_per_ws) + 1;

-- Update each project's issue_number to reflect actual issue count
UPDATE project p
SET issue_number = (SELECT count(*) FROM issue i WHERE i.project_id = p.id);

COMMIT;

ANALYZE;

\echo '======================================================'
\echo 'Done. Counts:'
\echo '======================================================'
SELECT 'members'           AS table, count(*) FROM member
UNION ALL SELECT 'workspaces',          count(*) FROM workspace
UNION ALL SELECT 'workspace_members',   count(*) FROM workspace_member
UNION ALL SELECT 'projects',            count(*) FROM project
UNION ALL SELECT 'project_members',     count(*) FROM project_member
UNION ALL SELECT 'workflows',           count(*) FROM workflow
UNION ALL SELECT 'workflow_states',     count(*) FROM workflow_state
UNION ALL SELECT 'issue_types',         count(*) FROM issue_type
UNION ALL SELECT 'issues',              count(*) FROM issue;
