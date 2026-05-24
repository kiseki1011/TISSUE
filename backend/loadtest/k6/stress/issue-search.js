// ============================================================
// Single-endpoint stress: workspace issue search (priority filter)
//
// Strategy: constant-arrival-rate — open-model load. Even if responses
// are slow, k6 keeps firing requests at the target rate, so you see the
// real backpressure (queue/latency growth) when capacity is reached.
//
// Override RATE/DURATION to find the breaking point:
//   docker run ... -e RATE=20  -e DURATION=1m  ...   (start here)
//   docker run ... -e RATE=50  -e DURATION=1m  ...
//   docker run ... -e RATE=100 -e DURATION=1m  ...
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { searchWorkspaceIssues } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '20');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 4).toString());

export const options = {
  scenarios: {
    issue_search_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                       ['rate<0.01'],
    'http_req_duration{op:issue_search}':    ['p(95)<500', 'p(99)<1500'],
  },
  tags: { testid: TESTID, stress: 'issue_search', target_rate: String(RATE) },
};

export function setup() {
  return { token: login() };
}

export default function (data) {
  searchWorkspaceIssues(authHeaders(data.token));
}
