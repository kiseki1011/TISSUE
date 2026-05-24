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

function get(path, op, headers) {
  const res = http.get(`${BASE}${path}`, { headers, tags: { op } });
  const ok = check(res, { [`${op} 2xx`]: r => r.status >= 200 && r.status < 300 });
  if (!ok) errorRate.add(1);
  return res;
}

// ---------- Workspace ----------
export function listWorkspaces(h)   { return get(`/api/v1/workspaces/me`, 'list_workspaces', h); }
export function getWorkspace(h)     { return get(`/api/v1/workspaces/${WORKSPACE_KEY}`, 'get_workspace', h); }

// ---------- Workspace member ----------
export function listMembers(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/members?page=0&size=20`, 'list_members', h); }
export function getMember(h)        { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/members/${pickMemberId()}`, 'get_member', h); }

// ---------- Project ----------
export function listProjects(h)     { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects?page=0&size=20`, 'list_projects', h); }
export function getProject(h)       { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}`, 'get_project', h); }
export function listProjectMembers(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/members?page=0&size=20`, 'list_project_members', h);
}

// ---------- Issue search (workspace + project scope) ----------
export function searchWorkspaceIssues(h) {
  const priorities = ['P0,P1', 'P2,P3', 'P0,P1,P2'][randomIntBetween(0, 2)];
  const page       = randomIntBetween(0, 50);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues?priorities=${priorities}&page=${page}&size=20`,
             'issue_search', h);
}
export function searchWorkspaceIssuesByKeyword(h) {
  const kws  = ['Issue', 'P0001', 'P0005', 'P0008', 'Issue 1234'];
  const kw   = kws[randomIntBetween(0, kws.length - 1)];
  const page = randomIntBetween(0, 5);
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues?keyword=${encodeURIComponent(kw)}&page=${page}&size=20`,
             'issue_search_keyword', h);
}
export function searchProjectIssues(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues?page=${randomIntBetween(0, 50)}&size=20`,
             'project_issue_search', h);
}

// ---------- Issue detail (per-aspect) ----------
export function issueBasic(h)       { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/basic`,       'issue_basic',       h); }
export function issueCommon(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/common`,      'issue_common',      h); }
export function issueParent(h)      { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/parent`,      'issue_parent',      h); }
export function issueChildren(h)    { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/children`,    'issue_children',    h); }
export function issueRelations(h)   { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/relations`,   'issue_relations',   h); }
export function issueReviewers(h)   { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/reviewers`,   'issue_reviewers',   h); }
export function issueSubscribers(h) { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/subscribers`, 'issue_subscribers', h); }
export function issueTransitions(h) { return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/transitions`, 'issue_transitions', h); }

// ---------- Workflow ----------
export function listProjectWorkflows(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/workflows`, 'list_workflows', h);
}
// Workflow GET is now nested under projects (see backend WorkflowQueryController).
// Caller obtains (projectKey, id) tuples from lib/discover.js#discoverWorkflows.
export function getWorkflow(h, projectKey, id) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${projectKey}/workflows/${id}`, 'get_workflow', h);
}

// ---------- Issue type ----------
export function listProjectIssueTypes(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issue-types`, 'list_issue_types', h);
}
// IssueType GET is now nested under projects. discoverIssueTypes returns (id, projectKey) tuples.
export function getIssueType(h, projectKey, id) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${projectKey}/issue-types/${id}`, 'get_issue_type', h);
}
