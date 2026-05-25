// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Single-endpoint stress: project-scoped issue search
//
// Compared to workspace-scoped search, this has a stricter filter
// (single project_id) and should hit the (project_id, priority, due_at)
// index efficiently. Use as a control to isolate workspace-fanout cost.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { searchProjectIssues } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '50');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    project_issue_search_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                                  ['rate<0.01'],
    'http_req_duration{op:project_issue_search}':       ['p(95)<400', 'p(99)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_search', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  searchProjectIssues(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
