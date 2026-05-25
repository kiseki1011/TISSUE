// ============================================================
// Single-endpoint stress: project-scoped FTS with keyset cursor pagination.
//
// Counterpart of project-issue-search-fts-multi.js — same keyword pool, same
// project scope, but the cursor endpoint avoids the count query and OFFSET
// penalty entirely. Direct A/B to quantify the keyset speedup.
//
// Each iteration fetches the FIRST page only (no cursor token reuse) so the
// per-request distribution is clean. Use a separate scenario if you want to
// measure deep-page chaining.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { ftsProjectIssuesCursor } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '60s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    project_issue_fts_cursor_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                              ['rate<0.05'],
    'http_req_duration{op:project_issue_fts_cursor}': ['p(95)<200', 'p(99)<500'],
  },
  tags: { testid: TESTID, stress: 'project_issue_fts_cursor', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  ftsProjectIssuesCursor(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
