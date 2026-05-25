// ============================================================
// Single-endpoint stress: workspace members list (lightweight read).
// Good for finding the raw HTTP throughput ceiling of the stack.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { listMembers } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '200');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE).toString());

export const options = {
  scenarios: {
    list_members_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                     ['rate<0.01'],
    'http_req_duration{op:list_members}':  ['p(95)<200', 'p(99)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'list_members', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  listMembers(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
