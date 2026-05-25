// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Stress: get issue type detail (single type with fields + options).
//
// IDs are discovered at setup() (see workflow-get.js for rationale).
// ============================================================

import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { discoverIssueTypes } from '../lib/discover.js';
import { getIssueType } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '100');
const DURATION = __ENV.DURATION || '30s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    issue_type_get_stress: {
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
    'http_req_duration{op:get_issue_type}':  ['p(95)<150', 'p(99)<400'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'get_issue_type', target_rate: String(RATE) },
};

export function setup() {
  const token = login();
  const issueTypes = discoverIssueTypes(token);
  console.log(`✓ discovered ${issueTypes.length} issue types`);
  return { token, issueTypes };
}

export default function (data) {
  const { id, projectKey } = data.issueTypes[randomIntBetween(0, data.issueTypes.length - 1)];
  getIssueType(authHeaders(data.token), projectKey, id);
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
