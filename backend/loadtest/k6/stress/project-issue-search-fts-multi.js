// ============================================================
// Single-endpoint stress: project-scoped FTS with 2-word query.
//
// Two-term plainto_tsquery is selective enough that the planner uses the
// GIN bitmap instead of a parallel seq scan, which is where tsvector
// actually beats LIKE.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { ftsProjectIssuesMulti } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    project_issue_fts_multi_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                                  ['rate<0.05'],
    'http_req_duration{op:project_issue_fts_multi}':    ['p(95)<400', 'p(99)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_fts_multi', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  ftsProjectIssuesMulti(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
