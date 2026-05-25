// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Single-endpoint stress: project-scoped issue full-text search.
//
// Counterpart to project-issue-search-keyword.js. Higher default RATE
// than the workspace variant since project scope is more selective.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { ftsProjectIssues } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    project_issue_fts_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                            ['rate<0.05'],
    'http_req_duration{op:project_issue_fts}':    ['p(95)<400', 'p(99)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_fts', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  ftsProjectIssues(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
