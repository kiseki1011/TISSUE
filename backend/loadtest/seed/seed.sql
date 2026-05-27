-- ============================================================
-- AI-GENERATED
-- model: claude-opus-4-7
-- NOT REVIEWED
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
--   * summary left NULL. custom_fields populated by Phase B.
--
-- Approximate row counts at 1M-issue scale:
--   issue:            1M     activity_log:    15M     comment:        3M
--   issue_subscriber: 1M     issue_reviewer:  1M      issue_tag:      1M
--   notification:     5M     wiki_document:  333K    issue_field:     5K
--   field_option:     4K     tag:             5K     sprint:          3K
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

-- Capture project_member ids per project (for issue.assignee_id) and the
-- backing member_id (for subscriber/reviewer/comment.author_id which point
-- to member, not project_member).
CREATE TEMP TABLE _pm AS
SELECT pm.id AS project_member_id, pm.project_id, pm.member_id,
       row_number() OVER (PARTITION BY pm.project_id ORDER BY pm.id) AS pm_idx
FROM project_member pm;

-- TEMP tables get no stats / no indexes by default; add what the seed JOINs need.
CREATE INDEX ON _pm (project_id, pm_idx);
CREATE INDEX ON _proj (project_id);
ANALYZE _proj;
ANALYZE _pm;

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

-- IssueType: one STANDARD type per project, bound to its FIRST workflow.
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

-- 5 fields per issue_type (TEXT, INTEGER, SELECT_OPTION, TEXT, DATE)
INSERT INTO issue_field (
    issue_type_id, display_name, normalized_name, description,
    issue_field_type, required, position
)
SELECT it.issue_type_id,
       fdef.display_name, fdef.normalized_name, fdef.description,
       fdef.field_type, fdef.required, fdef.position
FROM _it it
CROSS JOIN (VALUES
    ('Severity Description', 'severity_description', 'Impact description',  'TEXT',          false, 1),
    ('Estimate Hours',       'estimate_hours',       'Hours estimate',      'INTEGER',       false, 2),
    ('Severity',             'severity',             'Severity level',      'SELECT_OPTION', false, 3),
    ('Root Cause',           'root_cause',           'Root cause text',     'TEXT',          false, 4),
    ('Target Date',          'target_date',          'Target completion',   'DATE',          false, 5)
) AS fdef(display_name, normalized_name, description, field_type, required, position);

-- Pivot field ids per issue_type (one row per type, 5 columns)
CREATE TEMP TABLE _if_pivot AS
SELECT it.issue_type_id,
       MAX(f.id) FILTER (WHERE f.position = 1) AS f1_id,
       MAX(f.id) FILTER (WHERE f.position = 2) AS f2_id,
       MAX(f.id) FILTER (WHERE f.position = 3) AS f3_id,
       MAX(f.id) FILTER (WHERE f.position = 4) AS f4_id,
       MAX(f.id) FILTER (WHERE f.position = 5) AS f5_id
FROM _it it
JOIN issue_field f ON f.issue_type_id = it.issue_type_id
GROUP BY it.issue_type_id;
CREATE UNIQUE INDEX ON _if_pivot (issue_type_id);
ANALYZE _if_pivot;

-- 4 options on the SELECT_OPTION field only (field at position=3)
INSERT INTO field_option (issue_field_id, display_name, normalized_name)
SELECT p.f3_id, opt.display_name, opt.normalized_name
FROM _if_pivot p
CROSS JOIN (VALUES
    ('Low',      'low'),
    ('Medium',   'medium'),
    ('High',     'high'),
    ('Critical', 'critical')
) AS opt(display_name, normalized_name);

-- issue.custom_fields JSONB — deterministic values keyed by field id strings
UPDATE issue i
SET custom_fields = jsonb_build_object(
    p.f1_id::text, 'severity desc ' || (i.id % 100),
    p.f2_id::text, ((i.id % 40) + 1),
    p.f3_id::text, (ARRAY['Low','Medium','High','Critical'])[((i.id % 4) + 1)],
    p.f4_id::text, 'root cause sample ' || (i.id % 50),
    p.f5_id::text, to_char(date '2025-01-01' + ((i.id % 365)::int), 'YYYY-MM-DD')
)
FROM _if_pivot p
WHERE p.issue_type_id = i.issue_type_id;

-- Activity log: 10 events per issue mirroring a real lifecycle
-- (created → assigned → transitions → review cycle → comments → updates).
-- Total rows = 10 * issue_count. project_key derived from issue_key prefix.
-- Note: issue.assignee_id is a project_member.id, not member.id, so we don't
-- pretend actor_member_id is the assignee. Left NULL for simplicity.
-- COMMENT_ADDED count (3) matches comment INSERT below.
INSERT INTO activity_log (
    event_id, activity_type, resource_type, workspace_key,
    resource_id, project_key, issue_key,
    activity_data, changes
)
SELECT
    gen_random_uuid(),
    al.activity_type,
    'ISSUE',
    i.workspace_key,
    i.id,
    split_part(i.issue_key, '-', 1),
    i.issue_key,
    '{}'::jsonb,
    '{}'::jsonb
FROM issue i
CROSS JOIN (VALUES
    ('ISSUE_CREATED'),
    ('ISSUE_ASSIGNED'),
    ('ISSUE_WORKFLOW_TRANSITIONED'),
    ('ISSUE_REVIEWER_ADDED'),
    ('ISSUE_REVIEW_REQUESTED'),
    ('ISSUE_REVIEW_SUBMITTED'),
    ('ISSUE_COMMENT_ADDED'),
    ('ISSUE_COMMENT_ADDED'),
    ('ISSUE_COMMENT_ADDED'),
    ('ISSUE_UPDATED'),
    ('ISSUE_TAG_ADDED'),
    ('ISSUE_SUBSCRIBER_ADDED'),
    ('ISSUE_PRIORITY_CHANGED'),
    ('ISSUE_PARENT_CHANGED'),
    ('ISSUE_ATTACHMENT_ADDED')
) AS al(activity_type);

-- issue_subscriber: 1 per issue
INSERT INTO issue_subscriber (
    issue_id, workspace_key, issue_key, subscriber_id, subscribed_at
)
SELECT
    i.id, i.workspace_key, i.issue_key, pm.member_id, NOW()
FROM issue i
JOIN _pm pm ON pm.project_id = i.project_id
            AND pm.pm_idx = (((i.id + 3) % :members_per_ws) + 1);

-- issue_reviewer: 1 per issue, different member from subscriber (offset by half).
INSERT INTO issue_reviewer (
    issue_id, workspace_key, issue_key, reviewer_id, status
)
SELECT
    i.id, i.workspace_key, i.issue_key, pm.member_id,
    (ARRAY['PENDING','APPROVED','CHANGES_REQUESTED'])[((i.id % 3) + 1)]
FROM issue i
JOIN _pm pm ON pm.project_id = i.project_id
            AND pm.pm_idx = (((i.id + (:members_per_ws / 2)) % :members_per_ws) + 1);

-- notification: 5 per issue (receiver/actor rotated among project members).
-- Approximates an issue-driven inbox feed:
--   ISSUE_ASSIGNED, ISSUE_UPDATED, ISSUE_COMMENT_ADDED, ISSUE_MENTIONED, ISSUE_REVIEWER_ADDED
INSERT INTO notification (
    event_id, receiver_member_id, receiver_language, notification_type,
    is_read, resource_type, workspace_key,
    issue_key, project_key, actor_member_id,
    actor_display_name, message_data
)
SELECT
    gen_random_uuid(),
    pm_recv.member_id,
    'EN',
    nt.notification_type,
    ((i.id + nt.n) % 3 = 0),
    'ISSUE',
    i.workspace_key,
    i.issue_key,
    split_part(i.issue_key, '-', 1),
    pm_actor.member_id,
    'Load User',
    '{}'::jsonb
FROM issue i
CROSS JOIN (VALUES
    (1, 'ISSUE_ASSIGNED'),
    (2, 'ISSUE_UPDATED'),
    (3, 'ISSUE_COMMENT_ADDED'),
    (4, 'ISSUE_MENTIONED'),
    (5, 'ISSUE_REVIEWER_ADDED')
) AS nt(n, notification_type)
JOIN _pm pm_recv  ON pm_recv.project_id  = i.project_id
                 AND pm_recv.pm_idx  = (((i.id + nt.n)        % :members_per_ws) + 1)
JOIN _pm pm_actor ON pm_actor.project_id = i.project_id
                 AND pm_actor.pm_idx = (((i.id + nt.n + 7)    % :members_per_ws) + 1);

-- tag: 5 tags per project (bug, feature, urgent, frontend, backend)
INSERT INTO tag (
    project_id, project_key, workspace_key, display_name, normalized_name,
    description, color
)
SELECT pr.project_id, pr.project_key, pr.workspace_key,
       td.display_name, td.normalized_name, td.description, td.color
FROM _proj pr
CROSS JOIN (VALUES
    ('Bug',      'bug',      'Defect',          'RED'),
    ('Feature',  'feature',  'New feature',     'BLUE'),
    ('Urgent',   'urgent',   'Needs priority',  'BRIGHT_RED'),
    ('Frontend', 'frontend', 'UI work',         'CYAN'),
    ('Backend',  'backend',  'API/DB work',     'GREEN')
) AS td(display_name, normalized_name, description, color);

-- Capture tag ids per project + position (1..5)
CREATE TEMP TABLE _tag AS
SELECT t.id AS tag_id, t.project_id,
       row_number() OVER (PARTITION BY t.project_id ORDER BY t.id) AS tag_idx
FROM tag t
JOIN _proj pr ON pr.project_id = t.project_id;
CREATE INDEX ON _tag (project_id, tag_idx);
ANALYZE _tag;

-- issue_tag: each issue gets 1 deterministic tag (id % 5 → tag_idx 1..5)
INSERT INTO issue_tag (
    issue_id, tag_id, workspace_key, issue_key
)
SELECT i.id, t.tag_id, i.workspace_key, i.issue_key
FROM issue i
JOIN _tag t ON t.project_id = i.project_id
            AND t.tag_idx = ((i.id % 5) + 1);

-- sprint: 3 per project (PLANNING + ACTIVE + COMPLETED)
INSERT INTO sprint (
    project_id, workspace_key, project_key, sprint_number, title,
    sprint_status, started_at, due_at, completed_at, goal,
    archived, soft_deleted
)
SELECT pr.project_id, pr.workspace_key, pr.project_key,
       sd.sprint_number, sd.title, sd.sprint_status,
       sd.started_at::timestamptz, sd.due_at::timestamptz, sd.completed_at::timestamptz,
       sd.goal,
       false, false
FROM _proj pr
CROSS JOIN (VALUES
    (1, 'Sprint 1',       'COMPLETED', '2024-12-01', '2024-12-15', '2024-12-15', 'Q4 closeout'),
    (2, 'Sprint 2',       'ACTIVE',    '2025-01-01', '2025-01-15',  NULL,        'Q1 kickoff'),
    (3, 'Sprint 3 plan',  'PLANNING',   NULL,        '2025-02-01',  NULL,        'Upcoming')
) AS sd(sprint_number, title, sprint_status, started_at, due_at, completed_at, goal);

-- Assign sprint to half of the issues: even ids → ACTIVE sprint of their project
UPDATE issue i
SET sprint_id = s.id
FROM sprint s
WHERE s.project_id = i.project_id
  AND s.sprint_status = 'ACTIVE'
  AND (i.id % 2) = 0;

-- comment: 3 comments per issue with rotating authors (deterministic).
-- author_id references member.id (NOT project_member.id) — look up via _pm.
-- Matches the ISSUE_COMMENT_ADDED count in the activity_log INSERT above.
INSERT INTO comment (
    content, author_id, issue_id, workspace_key, issue_key,
    is_edited, archived, soft_deleted
)
SELECT
    'Comment ' || n || ' for ' || i.issue_key,
    pm.member_id,
    i.id,
    i.workspace_key,
    i.issue_key,
    false, false, false
FROM issue i
CROSS JOIN generate_series(1, 3) AS n
JOIN _pm pm ON pm.project_id = i.project_id
            AND pm.pm_idx = (((i.id + n * 7) % :members_per_ws) + 1);

-- Update each project's issue_number to reflect actual issue count
UPDATE project p
SET issue_number = (SELECT count(*) FROM issue i WHERE i.project_id = p.id);

-- ------------------------------------------------------------
-- Wiki documents — ~1/3 of issue count per workspace, ≥ 1KB content each.
-- Single-INSERT path (CTE picks workspace owner once per ws).
-- ------------------------------------------------------------
WITH _ws_owner AS (
    SELECT w.workspace_id, w.workspace_key,
           (SELECT min(wm.member_id) FROM workspace_member wm
            WHERE wm.workspace_id = w.workspace_id AND wm.workspace_role = 'OWNER') AS owner_id
    FROM _ws w
)
INSERT INTO wiki_document (
    workspace_id, workspace_key, title, content, locked,
    major_version, minor_version, patch_version,
    archived, soft_deleted, version,
    created_by, created_at, last_modified_by, last_modified_at
)
SELECT
    o.workspace_id,
    o.workspace_key,
    'Wiki ' || o.workspace_key || '-' || lpad(n::text, 6, '0'),
    'Wiki #' || n || ' of ' || o.workspace_key || '. ' ||
        repeat(
            'Tissue is a self-hosted issue tracker. This wiki document covers '
            || 'the workflow, sprint planning, retrospective notes, deployment runbook, '
            || 'oncall handoff, and team policy. Keywords: deploy docker kubernetes oauth jwt '
            || 'cache redis postgres queue kafka metric alert sprint backlog review comment. ',
            8
        ),
    false,
    1, 0, 0,
    false, false, 0,
    o.owner_id, now(), o.owner_id, now()
FROM _ws_owner o
CROSS JOIN generate_series(1, (:proj_per_ws * :issues_per_proj / 3)) AS n;

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
UNION ALL SELECT 'issue_fields',        count(*) FROM issue_field
UNION ALL SELECT 'field_options',       count(*) FROM field_option
UNION ALL SELECT 'issues',              count(*) FROM issue
UNION ALL SELECT 'activity_logs',       count(*) FROM activity_log
UNION ALL SELECT 'notifications',       count(*) FROM notification
UNION ALL SELECT 'issue_subscribers',   count(*) FROM issue_subscriber
UNION ALL SELECT 'issue_reviewers',     count(*) FROM issue_reviewer
UNION ALL SELECT 'comments',            count(*) FROM comment
UNION ALL SELECT 'tags',                count(*) FROM tag
UNION ALL SELECT 'issue_tags',          count(*) FROM issue_tag
UNION ALL SELECT 'sprints',             count(*) FROM sprint
UNION ALL SELECT 'wiki_documents',      count(*) FROM wiki_document;
