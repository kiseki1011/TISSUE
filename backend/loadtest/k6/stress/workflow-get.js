// ============================================================
// Stress: get workflow detail (states + transitions graph).
// Hottest workflow endpoint — called on every issue transition,
// every status change preview, etc.
//
// IDs are discovered at setup() because PostgreSQL parallel INSERT
// scatters identity ids; hard-coded ranges are not reliable.
// ============================================================

import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { discoverWorkflows } from '../lib/discover.js';
import { getWorkflow } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '100');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    workflow_get_stress: {
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
    'http_req_duration{op:get_workflow}':    ['p(95)<150', 'p(99)<400'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'get_workflow', target_rate: String(RATE) },
};

export function setup() {
  const token = login();
  const workflows = discoverWorkflows(token);
  console.log(`✓ discovered ${workflows.length} workflows`);
  return { token, workflows };
}

export default function (data) {
  const { id, projectKey } = data.workflows[randomIntBetween(0, data.workflows.length - 1)];
  getWorkflow(authHeaders(data.token), projectKey, id);
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
