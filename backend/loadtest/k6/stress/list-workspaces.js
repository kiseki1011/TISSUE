// ============================================================
// Single-endpoint stress: GET /workspaces/me
//
// One of the lightest, highest-throughput endpoints — used here to find
// the raw HTTP/Tomcat/JWT-decode throughput ceiling of the stack.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { listWorkspaces } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '300');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE).toString());

export const options = {
  scenarios: {
    list_workspaces_stress: {
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
    'http_req_duration{op:list_workspaces}':    ['p(95)<150', 'p(99)<400'],
  },
  tags: { testid: TESTID, stress: 'list_workspaces', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  listWorkspaces(authHeaders(data.token));
}
