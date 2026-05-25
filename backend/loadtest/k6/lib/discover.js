// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Resolve real ids for WS0001 at setup() — necessary because PostgreSQL
// parallel INSERT scatters identity ids unpredictably, so hard-coded ranges
// in env.js can be wrong. Run once per scenario, cached for the run.

import http from 'k6/http';
import { check } from 'k6';
import { BASE, WORKSPACE_KEY, PROJECT_KEYS } from './env.js';

function authGet(token, path) {
  return http.get(`${BASE}${path}`, {
    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  });
}

// Returns array of { id, projectKey } — the projectKey is required
// because workflow GET path is now nested under projects.
export function discoverWorkflows(token) {
  const items = [];
  for (const pk of PROJECT_KEYS) {
    const res = authGet(token, `/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pk}/workflows`);
    check(res, { [`workflows list ${pk} 200`]: r => r.status === 200 });
    for (const w of res.json() || []) items.push({ id: w.id, projectKey: pk });
  }
  if (!items.length) throw new Error('No workflows discovered in WS0001 — seed missing?');
  return items;
}

// Returns array of { id, projectKey }
export function discoverIssueTypes(token) {
  const items = [];
  for (const pk of PROJECT_KEYS) {
    const res = authGet(token, `/api/v1/workspaces/${WORKSPACE_KEY}/projects/${pk}/issue-types`);
    check(res, { [`types list ${pk} 200`]: r => r.status === 200 });
    for (const t of res.json() || []) items.push({ id: t.id, projectKey: pk });
  }
  if (!items.length) throw new Error('No issue types discovered in WS0001 — seed missing?');
  return items;
}
