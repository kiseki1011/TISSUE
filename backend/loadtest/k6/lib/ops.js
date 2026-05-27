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
import { pickSingleKeyword, pickMultiKeyword } from './keywords.js';

export const errorRate = new Rate('errors');

function pickProject()  { return PROJECT_KEYS[randomIntBetween(0, PROJECT_KEYS.length - 1)]; }
function pickIssueKey() { return `${pickProject()}-${randomIntBetween(1, ISSUES_PER_PROJ)}`; }
// Only pick member ids that actually belong to WS0001 (the workspace we're testing)
function pickMemberId() { return randomIntBetween(1, MEMBERS_PER_WS); }

const PRIORITIES = ['LOW','MEDIUM','HIGH','URGENT'];
function pickPriority() { return PRIORITIES[randomIntBetween(0, PRIORITIES.length - 1)]; }

function get(path, op, headers) {
  const res = http.get(`${BASE}${path}`, { headers, tags: { op } });
  const ok = check(res, { [`${op} 2xx`]: r => r.status >= 200 && r.status < 300 });
  if (!ok) errorRate.add(1);
  return res;
}

// Generic write helper for POST/PUT/PATCH/DELETE.
// Body is JSON-stringified; pass null for empty body.
// 2xx includes 200/201/204
function send(method, path, op, headers, body) {
  const res = http.request(method, `${BASE}${path}`, body == null ? null : JSON.stringify(body),
                           { headers, tags: { op } });
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

// Issue search (single-word, worst-case selectivity)
export function searchIssuesSingle(h) {
  const kw = pickSingleKeyword();
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search?keyword=${encodeURIComponent(kw)}&size=20`,
             'issue_search_single', h);
}

// Issue search (two-word)
export function searchIssuesMulti(h) {
  const kw = pickMultiKeyword();
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/issues:search?keyword=${encodeURIComponent(kw)}&size=20`,
             'issue_search_multi', h);
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

// Tag / Sprint list (read-only - empty seed for now, exercises the controller path)
export function listProjectTags(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/tags`, 'list_tags', h);
}
export function listProjectSprints(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pickProject()}/sprints?page=0&size=20`, 'list_sprints', h);
}

// Comment list (read)
export function listIssueComments(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/comments?page=0&size=20`, 'list_comments', h);
}

// ============================================================
// Wiki - READ targets WS0001 (large seed, hundreds of thousands of rows).
//        WRITE targets WS_W (isolated; see further down).
// ============================================================
export function listWikiRoots(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/wiki/roots`, 'wiki_roots', h);
}
export function getWikiTree(h) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/wiki/tree`, 'wiki_tree', h);
}
export function searchWiki(h) {
  const kw = pickSingleKeyword();
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/wiki/search?query=${encodeURIComponent(kw)}&page=0&size=20`,
             'wiki_search', h);
}
export function getWikiDocument(h, wikiId) {
  return get(`/api/v1/workspaces/${WORKSPACE_KEY}/wiki/${wikiId}`, 'wiki_get', h);
}

// ============================================================
// Write ops - all target WS0001 (realistic latency on the read-sized dataset).
// k6-created rows are tagged with created_by = loadadmin, so cleanup.sql can
// remove only what k6 inserted without touching the seed.
// ============================================================
export function createComment(h) {
  return send('POST', `/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/comments`,
              'comment_create', h, { content: `loadtest comment ${Date.now()}` });
}

export function updateIssuePriority(h) {
  return send('PATCH', `/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}`,
              'issue_update', h, { priority: pickPriority() });
}

export function updateStoryPoint(h) {
  return send('PATCH', `/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/storypoint`,
              'issue_storypoint', h, { storyPoint: randomIntBetween(0, 21) });
}

// Toggle subscribe / unsubscribe on a random issue. Alternating prevents
// "already subscribed" 409s from inflating the error rate.
export function toggleSubscribeIssue(h) {
  const path = `/api/v1/workspaces/${WORKSPACE_KEY}/issues/${pickIssueKey()}/subscribers`;
  const method = Math.random() < 0.5 ? 'POST' : 'DELETE';
  return send(method, path, 'issue_subscribe', h, null);
}

// createIssue requires (projectKey, issueTypeId) discovered at setup() via
// discoverIssueTypes().
export function createIssue(h, projectKey, issueTypeId) {
  const n = randomIntBetween(1, 1_000_000);
  return send('POST', `/api/v1/workspaces/${WORKSPACE_KEY}/projects/${projectKey}/issues`,
              'issue_create', h, {
                title:       `loadtest issue ${n}`,
                content:     `created by k6 at ${Date.now()}`,
                priority:    pickPriority(),
                issueTypeId: issueTypeId,
              });
}

export function createWiki(h) {
  const n = randomIntBetween(1, 1_000_000);
  return send('POST', `/api/v1/workspaces/${WORKSPACE_KEY}/wiki`, 'wiki_create', h, {
    title:   `loadtest wiki ${n}`,
    content: `created by k6 at ${Date.now()}. Sample body for load test.`,
  });
}

export function updateWikiContent(h, wikiId) {
  return send('PATCH', `/api/v1/workspaces/${WORKSPACE_KEY}/wiki/${wikiId}/content`,
              'wiki_update', h, {
                content:           `updated at ${Date.now()}`,
                versionUpdateType: 'MINOR',
              });
}
