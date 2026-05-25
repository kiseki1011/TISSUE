// ============================================================
// Single-endpoint stress: issue relations (graph-style query)
//
// Why measure separately: relations involve self-FK + cross-project joins,
// which can hide N+1 problems that detail-by-basic doesn't expose.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { issueRelations } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '50');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    issue_relations_stress: {
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
    'http_req_duration{op:issue_relations}':    ['p(95)<300', 'p(99)<800'],
  },
  tags: { testid: TESTID, stress: 'issue_relations', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  issueRelations(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
