// ============================================================
// Single-endpoint stress: project-scoped issues filtered by author.
//
// "issues created by member X" — relies on issue.created_by audit column.
// No FTS, no LIKE — pure equality filter on the b-tree if/when an index
// on (project_id, created_by) exists. Useful as a baseline for the "look up
// a person's authored issues" use case (e.g. member detail hover card).
// ============================================================

import { TESTID } from '../lib/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { buildSummary } from '../lib/summary.js';
import { searchProjectIssuesByAuthor } from '../lib/ops.js';

const RATE     = parseInt(__ENV.RATE     || '50');
const DURATION = __ENV.DURATION || '60s';
const PRE_VUS  = parseInt(__ENV.PRE_VUS  || (RATE * 2).toString());

export const options = {
  scenarios: {
    project_issue_author_stress: {
      executor:           'constant-arrival-rate',
      rate:               RATE,
      timeUnit:           '1s',
      duration:           DURATION,
      preAllocatedVUs:    PRE_VUS,
      maxVUs:             PRE_VUS * 2,
    },
  },
  thresholds: {
    'http_req_failed':                            ['rate<0.01'],
    'http_req_duration{op:project_issue_author}': ['p(95)<400', 'p(99)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID, stress: 'project_issue_author', target_rate: String(RATE) },
};

export function setup() { return { token: login() }; }
export default function (data) {
  searchProjectIssuesByAuthor(authHeaders(data.token));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
