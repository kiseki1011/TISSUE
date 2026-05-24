// ============================================================
// Single-endpoint stress: workspace issue search with keyword (LIKE).
// Slow on purpose — useful to find at what RATE the system collapses.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { searchWorkspaceIssuesByKeyword } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '5');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || '100');

export const options = {
  scenarios: {
    issue_search_keyword_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 3,
    },
  },
  thresholds: {
    'http_req_failed':                                ['rate<0.05'],
    'http_req_duration{op:issue_search_keyword}':     ['p(95)<8000'],
  },
  tags: { testid: TESTID, stress: 'issue_search_keyword', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  searchWorkspaceIssuesByKeyword(authHeaders(data.token));
}
