// ============================================================
// Stress: list workflows per project.
// Hot path — UI shows workflow picker whenever opening a project.
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { listProjectWorkflows } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '100');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    workflow_list_stress: {
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
    'http_req_duration{op:list_workflows}':  ['p(95)<200', 'p(99)<500'],
  },
  tags: { testid: TESTID, stress: 'list_workflows', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  listProjectWorkflows(authHeaders(data.token));
}
