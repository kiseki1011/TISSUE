// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Single-endpoint stress: project-scoped FTS by issue_key token.
//
// "P0001-1234" tokenizes (under simple config) into {p0001, 1234} and
// matches the GIN index with very high selectivity (1 issue out of millions).
// Direct equivalent of "navigate by typing the issue id".
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { ftsProjectIssuesByKey } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '50');
const DURATION = __ENV.DURATION || '60s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    project_issue_fts_key_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                          ['rate<0.01'],
    'http_req_duration{op:project_issue_fts_key}': ['p(95)<200', 'p(99)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_fts_key', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  ftsProjectIssuesByKey(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
