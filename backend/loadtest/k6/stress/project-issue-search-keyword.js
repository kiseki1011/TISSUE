// ============================================================
// Single-endpoint stress: project-scoped issue search with keyword (LIKE).
//
// More realistic than workspace-wide keyword search: typical UI scopes by
// project. Selectivity is much higher (single project_id), so we can drive
// a higher RATE than the workspace variant.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { searchProjectIssuesByKeyword } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    project_issue_search_keyword_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                                       ['rate<0.05'],
    'http_req_duration{op:project_issue_search_keyword}':    ['p(95)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_search_keyword', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  searchProjectIssuesByKeyword(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
