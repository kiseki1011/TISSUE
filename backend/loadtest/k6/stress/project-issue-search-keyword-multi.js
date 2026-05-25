// ============================================================
// Single-endpoint stress: project-scoped LIKE search with 2-word keyword.
//
// Multi-word LIKE is still a sequential ILIKE scan on the filtered rows,
// so this exists mostly as the direct comparand for the FTS-multi variant.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { searchProjectIssuesByMultiKeyword } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    project_issue_search_multi_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                                     ['rate<0.05'],
    'http_req_duration{op:project_issue_search_multi}':    ['p(95)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_search_multi', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  searchProjectIssuesByMultiKeyword(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
