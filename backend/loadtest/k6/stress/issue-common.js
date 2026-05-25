// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Single-endpoint stress: issue "common" detail (embeddable + author info etc.)
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { issueCommon } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '50');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    issue_common_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                      ['rate<0.01'],
    'http_req_duration{op:issue_common}':   ['p(95)<200', 'p(99)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'issue_common', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  issueCommon(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
