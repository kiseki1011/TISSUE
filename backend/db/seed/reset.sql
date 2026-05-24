-- ============================================================
-- Reset: truncate seeded data and reset identity sequences.
-- Safe to run between seed runs at different scales.
--
-- Usage:
--   docker exec -i tissue-loadtest-db psql -U tissue -d tissue \
--     -f /seed/reset.sql
-- ============================================================

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- CASCADE handles all FK-dependent tables in one shot.
-- RESTART IDENTITY resets the IDENTITY sequences back to 1.
TRUNCATE TABLE
    member,
    workspace,
    workspace_member,
    project,
    project_member,
    workflow,
    workflow_state,
    workflow_transition,
    issue_type,
    issue_field,
    field_option,
    issue,
    issue_subscriber,
    issue_reviewer,
    issue_tag,
    issue_relation,
    issue_branch,
    issue_attachment,
    tag,
    sprint,
    comment,
    activity_log,
    notification,
    notification_preference,
    auth_identity,
    refresh_token,
    email_verification_token,
    invitation,
    workspace_invite_link,
    workspace_member_position,
    workspace_member_team,
    workspace_vcs_integration,
    position,
    team,
    project_template,
    transition_guard_config,
    wiki_document,
    wiki_document_snapshot,
    wiki_link,
    wiki_bookmark,
    wiki_attachment,
    failed_email
RESTART IDENTITY CASCADE;

COMMIT;

\echo 'Reset complete. All tables truncated, identity sequences reset.'
