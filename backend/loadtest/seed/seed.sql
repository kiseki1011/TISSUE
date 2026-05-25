-- ============================================================
-- Tissue loadtest seed
--
-- Usage (defaults = 10k issues, smoke):
--   docker exec -i tissue-loadtest-db psql -U tissue -d tissue \
--     -v ws_count=10 -v members_per_ws=20 -v proj_per_ws=10 -v issues_per_proj=100 \
--     -f /seed/seed.sql
--
-- Scales (Profile A: large org):
--   10k:    ws=10  members_per_ws=20  proj_per_ws=10  issues_per_proj=100
--   1M:     ws=100 members_per_ws=100 proj_per_ws=10  issues_per_proj=1000
--   10M:    ws=100 members_per_ws=100 proj_per_ws=10  issues_per_proj=10000
--
-- Notes
--   * Members are domain rows only. They have no auth_identity.
--     For k6 auth, create test admin separately via API.
--   * Each project gets 2 workflows (Default + Bug Tracking), each with 4 states.
--     issue_type binds to the project's FIRST workflow.
--   * title is randomized from a vocabulary of ~80 tech words so keyword
--     search has signal. content is 5-10 words drawn from the same pool.
--   * summary and custom_fields left NULL.
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

-- Members (no auth_identity, cannot login)
INSERT INTO member (email, username, name, language, system_role, member_status)
SELECT
    'load' || i || '@loadtest.local',
    'load' || i,
    'Load User ' || i,
    'EN',
    'USER',
    'ACTIVE'
FROM generate_series(1, :ws_count * :members_per_ws) AS s(i);

-- Workspaces
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

-- WorkspaceMember (each member belongs to exactly one workspace)
-- First is OWNER, rest MEMBER
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


-- Projects
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

-- ProjectMember (every workspace_member joins every project of that workspace as MEMBER.
-- the OWNER becomes MANAGER)
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
-- Workflows: 2 per project ("Default" + "Bug Tracking")
-- ORDER BY ensures predictable ids (WS0001 workflows = ids 1..proj_per_ws*2).
-- ------------------------------------------------------------
INSERT INTO workflow (
    color, description, display_name, normalized_name,
    project_key, workspace_key, system_provided, version, project_id
)
SELECT
    CASE wn WHEN 1 THEN 'BLUE' ELSE 'GREEN' END,
    CASE wn WHEN 1 THEN 'Default workflow' ELSE 'Bug tracking workflow' END,
    CASE wn WHEN 1 THEN 'Default'          ELSE 'Bug Tracking'          END,
    CASE wn WHEN 1 THEN 'default'          ELSE 'bug_tracking'          END,
    pr.project_key, pr.workspace_key, false, 0, pr.project_id
FROM (SELECT project_id, project_key, workspace_key FROM _proj ORDER BY project_id) pr
CROSS JOIN generate_series(1, 2) AS wn
ORDER BY pr.project_id, wn;

-- Capture workflows + their ordinal within each project
CREATE TEMP TABLE _wf AS
SELECT w.id AS workflow_id, w.project_id, w.normalized_name,
       row_number() OVER (PARTITION BY w.project_id ORDER BY w.id) AS wf_idx
FROM workflow w
WHERE w.project_id IS NOT NULL;
CREATE INDEX ON _wf (project_id, wf_idx);
ANALYZE _wf;

-- 4 states (INITIAL/ACTIVE/COMPLETED/ABORTED) per workflow
INSERT INTO workflow_state (
    state_category, color, description, display_name, normalized_name,
    workflow_id, version
)
SELECT s.cat, s.color, s.descr, s.disp, s.norm, w.workflow_id, 0
FROM _wf w
CROSS JOIN (VALUES
    ('INITIAL',   'GRAY',  'Initial',     'To Do',       'to_do'),
    ('ACTIVE',    'BLUE',  'In progress', 'In Progress', 'in_progress'),
    ('COMPLETED', 'GREEN', 'Done',        'Done',        'done'),
    ('ABORTED',   'RED',   'Cancelled',   'Cancelled',   'cancelled')
) AS s(cat, color, descr, disp, norm);

-- Wire workflow.initial_state_id → its own INITIAL state
UPDATE workflow w
SET initial_state_id = s.id
FROM workflow_state s
WHERE s.workflow_id = w.id
  AND s.state_category = 'INITIAL'
  AND w.project_id IS NOT NULL;

-- Re-capture workflows now that initial_state_id is populated
DROP TABLE _wf;
CREATE TEMP TABLE _wf AS
SELECT w.id AS workflow_id, w.project_id, w.normalized_name, w.initial_state_id,
       row_number() OVER (PARTITION BY w.project_id ORDER BY w.id) AS wf_idx
FROM workflow w
WHERE w.project_id IS NOT NULL;
CREATE INDEX ON _wf (project_id, wf_idx);
ANALYZE _wf;

-- ------------------------------------------------------------
-- IssueType: one STANDARD type per project, bound to its FIRST workflow.
-- ------------------------------------------------------------
INSERT INTO issue_type (
    project_id, workflow_id,
    color, icon, hierarchy, description,
    display_name, normalized_name, system_provided, version
)
SELECT
    pr.project_id, w.workflow_id,
    'BLUE', 'CIRCLE_FILLED', 'STANDARD',
    'Default task',
    'Task', 'task',
    true, 0
FROM (SELECT project_id FROM _proj ORDER BY project_id) pr
JOIN _wf w ON w.project_id = pr.project_id AND w.wf_idx = 1;

-- Capture issue_type ids together with the matching initial_state of its workflow.
CREATE TEMP TABLE _it AS
SELECT it.id AS issue_type_id, it.project_id, w.initial_state_id
FROM issue_type it
JOIN _wf w ON w.project_id = it.project_id AND w.wf_idx = 1;
CREATE INDEX ON _it (project_id);
ANALYZE _it;

-- ------------------------------------------------------------
-- Issues (set-based, no per-row subqueries)
--   Per project: issues_per_proj issues, issue_key = "P<NNNN>-<n>"
--   current_state_id = its issue_type's workflow's initial state (per-project)
--
-- Vocabulary: 80 tech words held in an array literal (vocab[1..80]).
--   title   = 3 words joined by ' '   ("login session deploy")
--   content = 6 more words joined ' ' ("queue kafka stream batch metric trace")
-- Picks are deterministic in (project_id, n) → same row gets same text on re-run.
-- Array indexing is ~100x cheaper than a CTE/subquery per row at 10M scale.
-- ------------------------------------------------------------
WITH vocab(arr) AS (
    SELECT ARRAY[
        'login','signup','password','token','session',
        'cache','redis','postgres','sqlite','mysql',
        'queue','kafka','rabbit','stream','batch',
        'deploy','rollback','canary','staging','prod',
        'docker','kubernetes','pod','helm','cluster',
        'network','firewall','router','gateway','proxy',
        'metric','trace','span','log','alert',
        'dashboard','panel','chart','widget','report',
        'webhook','event','notification','email','slack',
        'oauth','jwt','saml','ldap','permission',
        'export','import','migration','schema','backup',
        'snapshot','restore','archive','cleanup','retention',
        'search','filter','sort','paginate','index',
        'upload','download','attachment','preview','thumbnail',
        'sprint','backlog','epic','story','bug',
        'review','comment','mention','reaction','edit'
    ]::text[]
)
INSERT INTO issue (
    project_id, workspace_key, issue_type_id, current_state_id, assignee_id,
    issue_key, title, content, priority, story_point,
    count_based_progress, point_based_progress,
    archived, soft_deleted, version
)
SELECT
    pr.project_id,
    pr.workspace_key,
    it.issue_type_id,
    it.initial_state_id,
    pm.project_member_id,
    pr.project_key || '-' || n,
    v.arr[((pr.project_id *  7 + n * 11) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 13 + n * 17) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 19 + n * 23) % 80) + 1],
    v.arr[((pr.project_id * 29 + n * 31) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 37 + n * 41) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 43 + n * 47) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 53 + n * 59) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 61 + n * 67) % 80) + 1] || ' ' ||
    v.arr[((pr.project_id * 71 + n * 73) % 80) + 1],
    (ARRAY['P0','P1','P2','P3','P4'])[((n - 1) % 5) + 1],
    ((n - 1) % 13) + 1,
    0, 0, false, false, 0
FROM _proj pr
CROSS JOIN vocab v
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
