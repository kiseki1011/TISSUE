// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Tissue baseline load test scenario  (mixed, read-heavy)
//
// Endpoints covered:
//   * workspace:   list /me, get
//   * member:      list, get one
//   * project:     list, get, list-members
//   * issue search: project-scoped filter, LIKE keyword,
//                   FTS multi-word (offset), FTS multi-word (cursor)
//   * issue detail: basic, common, parent, children, relations,
//                   reviewers, subscribers, transitions
//
// Weighted distribution (read-heavy):
//   22%  project issue search (filter, no keyword)
//   14%  project issue search (LIKE keyword)
//   14%  project issue search (FTS multi-word)
//    8%  project issue search (FTS cursor / keyset)
//   12%  issue detail (basic|common)
//   12%  issue detail (relations|children|parent|transitions|reviewers|subscribers)
//    8%  member ops
//    6%  project ops
//    4%  workspace ops
//
// Login happens once in setup() and the JWT is reused (token TTL = 2h).
//
// Run locally (docker):
//   docker run --rm -i --network backend_default \
//     -v "$(pwd)/loadtest/k6:/scripts" \
//     -v "$(pwd)/loadtest/results:/results" \
//     grafana/k6:0.55.0 run \
//     -e BASE_URL=http://app:8080 -e VUS_MAX=20 -e DURATION=1m \
//     -e TESTID=run-006-baseline-v2 \
//     /scripts/baseline.js
//
// HTML/JSON report is written to loadtest/results/${TESTID}.{html,json}.
// To push raw timeseries to Prometheus, append:
//   -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
//   -e K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99),min,max,avg" \
//   --out experimental-prometheus-rw
// ============================================================

import { sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { TESTID } from './lib/env.js';
import { login, authHeaders } from './lib/auth.js';
import { buildSummary } from './lib/summary.js';
import {
  listWorkspaces, getWorkspace,
  listMembers, getMember,
  listProjects, getProject, listProjectMembers,
  searchProjectIssues, searchProjectIssuesByKeyword,
  ftsProjectIssuesMulti, ftsProjectIssuesCursor,
  issueBasic, issueCommon, issueParent, issueChildren,
  issueRelations, issueReviewers, issueSubscribers, issueTransitions,
} from './lib/ops.js';

const VUS_MAX  = parseInt(__ENV.VUS_MAX  || '20');
const DURATION = __ENV.DURATION || '1m';

export const options = {
  scenarios: {
    ramping_baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s',     target: Math.ceil(VUS_MAX * 0.2) },
        { duration: '1m',      target: Math.ceil(VUS_MAX * 0.5) },
        { duration: DURATION,  target: VUS_MAX               },
        { duration: '30s',     target: 0                     },
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: {
    'http_req_failed':                                   ['rate<0.01'],
    'errors':                                            ['rate<0.01'],
    // Read SLOs
    'http_req_duration{op:list_workspaces}':             ['p(95)<300'],
    'http_req_duration{op:get_workspace}':               ['p(95)<200'],
    'http_req_duration{op:list_members}':                ['p(95)<400'],
    'http_req_duration{op:get_member}':                  ['p(95)<200'],
    'http_req_duration{op:list_projects}':               ['p(95)<300'],
    'http_req_duration{op:get_project}':                 ['p(95)<200'],
    'http_req_duration{op:list_project_members}':        ['p(95)<400'],
    'http_req_duration{op:project_issue_search}':         ['p(95)<400'],
    'http_req_duration{op:project_issue_search_keyword}': ['p(95)<3000', 'p(99)<8000'],
    'http_req_duration{op:project_issue_fts_multi}':      ['p(95)<400',  'p(99)<1000'],
    'http_req_duration{op:project_issue_fts_cursor}':     ['p(95)<200',  'p(99)<500'],
    'http_req_duration{op:issue_basic}':                 ['p(95)<200'],
    'http_req_duration{op:issue_common}':                ['p(95)<300'],
    'http_req_duration{op:issue_relations}':             ['p(95)<300'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID },
};

export function setup() {
  const token = login();
  console.log(`✓ login ok (token len=${token.length}), testid=${TESTID}`);
  return { token };
}

export default function (data) {
  const h = authHeaders(data.token);
  const r = Math.random();

  if      (r < 0.22) { searchProjectIssues(h); }
  // project issue search (LIKE keyword)
  else if (r < 0.36) { searchProjectIssuesByKeyword(h); }
  // project issue search (FTS multi-word)
  else if (r < 0.50) { ftsProjectIssuesMulti(h); }
  // project issue search (FTS keyset cursor)
  else if (r < 0.58) { ftsProjectIssuesCursor(h); }
  else if (r < 0.70) {
    const f = [issueBasic, issueCommon][randomIntBetween(0, 1)];
    f(h);
  }
  else if (r < 0.82) {
    const f = [issueRelations, issueChildren, issueParent, issueTransitions,
               issueReviewers, issueSubscribers][randomIntBetween(0, 5)];
    f(h);
  }
  else if (r < 0.90) {
    (Math.random() < 0.5 ? listMembers : getMember)(h);
  }
  else if (r < 0.96) {
    const f = [listProjects, getProject, listProjectMembers][randomIntBetween(0, 2)];
    f(h);
  }
  else {
    (Math.random() < 0.5 ? listWorkspaces : getWorkspace)(h);
  }

  sleep(randomIntBetween(0, 1));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
