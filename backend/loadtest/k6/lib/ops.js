// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Endpoint helpers. Each function:
//   - sends one HTTP request
//   - tags with { op: '<name>' } so Prometheus/Grafana can split per-endpoint
//   - returns the http response (so the caller can record custom Trends if wanted)

import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { BASE, WORKSPACE_KEY, PROJECT_KEYS, ISSUES_PER_PROJ, MEMBERS_PER_WS,
         WORKFLOW_ID_MIN, WORKFLOW_ID_MAX, ISSUE_TYPES_PER_WS } from './env.js';

export const errorRate = new Rate('errors');

function pickProject()  { return PROJECT_KEYS[randomIntBetween(0, PROJECT_KEYS.length - 1)]; }
function pickIssueKey() { return `${pickProject()}-${randomIntBetween(1, ISSUES_PER_PROJ)}`; }
// Only pick member ids that actually belong to WS0001 (the workspace we're testing)
function pickMemberId() { return randomIntBetween(1, MEMBERS_PER_WS); }

// Keyword pool — drawn from the seed vocab (loadtest/seed/seed.sql `vocab` array).
// Each word appears in many but not all rows, so LIKE returns non-trivial result
// sets and FTS has something to rank. Adjust if the seed vocab changes.
const KEYWORDS = [
  'login','password','token','session','cache','postgres','queue','kafka',
  'deploy','docker','kubernetes','network','metric','alert','dashboard',
  'oauth','jwt','permission','migration','search','sprint','review','bug',
];
function pickKeyword() { return KEYWORDS[randomIntBetween(0, KEYWORDS.length - 1)]; }

// Picks a random issue_key inside WS0001 — used to measure FTS against
// the issue_key token (e.g. "P0001-1234") that gets included in search_vector.
function pickIssueKeyToken() {
  return `${pickProject()}-${randomIntBetween(1, ISSUES_PER_PROJ)}`;
}

// Multi-word keyword pool. Each phrase combines two words from the seed
// vocab. Selectivity is much lower (~1-2%) so a GIN index can actually
// prune the bitmap before ts_rank kicks in — without two terms, FTS often
// degenerates to a parallel seq scan on a 10M-row table.
const MULTI_KEYWORDS = [
  'login backup','deploy docker','sprint review','kafka stream','cache redis',
  'oauth jwt','metric alert','search filter','migration schema','review comment',
  'permission ldap','docker kubernetes','password token','dashboard panel','export import',
  'pod cluster','webhook event','sprint backlog','snapshot restore','queue batch',
];
function pickMultiKeyword() { return MULTI_KEYWORDS[randomIntBetween(0, MULTI_KEYWORDS.length - 1)]; }

function get(path, op, headers) {
  const res = http.get(`${BASE}${path}`, { headers, tags: { op } });
  const ok = check(res, { [`${op} 2xx`]: r => r.status >= 200 && r.status < 300 });
  if (!ok) errorRate.add(1);
  return res;
}

// Workspace
export function listWorkspaces(h)   { return get(`/api/v1/workspaces/me`, 'list_workspaces', h); }
export function getWorkspace(h)     { return get(`/api/v1/workspaces/${WORKSPACE_KEY}`, 'get_workspace', h); }

// Workspace member
export function listMembers(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/members?page=0&size=20`, 'list_members', h); }
export function getMember(h)        { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/members/${pickMemberId()}`, 'get_member', h); }

// Project
export function listProjects(h)     { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects?page=0&size=20`, 'list_projects', h); }
export function getProject(h)       { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}`, 'get_project', h); }
export function listProjectMembers(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/members?page=0&size=20`, 'list_project_members', h);
}

// Issue search (project scope only — workspace-wide search was removed,
// see the FTS load-test report).
export function searchProjectIssues(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues?page=${randomIntBetween(0, 50)}&size=20`,
             'project_issue_search', h);
}
export function searchProjectIssuesByKeyword(h) {
  const kw   = pickKeyword();
  const page = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues?keyword=${encodeURIComponent(kw)}&page=${page}&size=20`,
             'project_issue_search_keyword', h);
}

// Issue full-text search (tsvector)
// /issues:search-fts wraps PostgreSQL to_tsquery on a GIN-indexed
// search_vector column (issue_key + title + content). Same keyword pool and
// filter shape as the LIKE variant so the two are directly comparable.
export function ftsProjectIssues(h) {
  const kw   = pickKeyword();
  const page = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search-fts?keyword=${encodeURIComponent(kw)}&page=${page}&size=20`,
             'project_issue_fts', h);
}

// Multi-word variants — same endpoints, but with 2-word phrases that GIN
// can actually prune. Kept as separate ops/tags so dashboards can split
// single-word vs multi-word results cleanly.
// Author filter — "issues created by member X". Picks a random member id
// from the WS0001 pool so each request hits a different author. Used to
// measure the (project_id, created_by) access path.
export function searchProjectIssuesByAuthor(h) {
  const authorId = randomIntBetween(1, MEMBERS_PER_WS);
  const page     = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues?authorMemberIds=${authorId}&page=${page}&size=20`,
             'project_issue_author', h);
}

export function searchProjectIssuesByMultiKeyword(h) {
  const kw   = pickMultiKeyword();
  const page = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues?keyword=${encodeURIComponent(kw)}&page=${page}&size=20`,
             'project_issue_search_multi', h);
}
export function ftsProjectIssuesMulti(h) {
  const kw   = pickMultiKeyword();
  const page = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search-fts?keyword=${encodeURIComponent(kw)}&page=${page}&size=20`,
             'project_issue_fts_multi', h);
}
// Cursor variant — first-page only (no token reuse across iterations) so each
// iteration measures the cold cursor case. Production clients would chain
// requests but we want a clean per-request distribution here.
export function ftsProjectIssuesCursor(h) {
  const kw = pickMultiKeyword();
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search-fts-cursor?keyword=${encodeURIComponent(kw)}&size=20`,
             'project_issue_fts_cursor', h);
}
export function ftsProjectIssuesByKey(h) {
  const kw = pickIssueKeyToken();
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search-fts?keyword=${encodeURIComponent(kw)}&page=0&size=20`,
             'project_issue_fts_key', h);
}

// Issue detail (per-aspect)
export function issueBasic(h)       { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/basic`,       'issue_basic',       h); }
export function issueCommon(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/common`,      'issue_common',      h); }
export function issueParent(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/parent`,      'issue_parent',      h); }
export function issueChildren(h)    { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/children`,    'issue_children',    h); }
export function issueRelations(h)   { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/relations`,   'issue_relations',   h); }
export function issueReviewers(h)   { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/reviewers`,   'issue_reviewers',   h); }
export function issueSubscribers(h) { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/subscribers`, 'issue_subscribers', h); }
export function issueTransitions(h) { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/transitions`, 'issue_transitions', h); }

// Workflow
export function listProjectWorkflows(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/workflows`, 'list_workflows', h);
}
// Workflow GET is now nested under projects (see backend WorkflowQueryController).
// Caller obtains (projectKey, id) tuples from lib/discover.js#discoverWorkflows.
export function getWorkflow(h, projectKey, id) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${projectKey}/workflows/${id}`, 'get_workflow', h);
}

// Issue type
export function listProjectIssueTypes(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issue-types`, 'list_issue_types', h);
}
// IssueType GET is now nested under projects. discoverIssueTypes returns (id, projectKey) tuples.
export function getIssueType(h, projectKey, id) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${projectKey}/issue-types/${id}`, 'get_issue_type', h);
}
