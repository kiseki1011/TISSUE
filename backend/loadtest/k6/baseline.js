// ============================================================
// Tissue baseline load test scenario  (mixed, read-heavy)
//
// Endpoints covered (~17):
//   * workspace:   list /me, get
//   * member:      list, get one
//   * project:     list, get, list-members
//   * issue search: workspace-scoped (priority), workspace-scoped (keyword),
//                   project-scoped
//   * issue detail: basic, common, parent, children, relations,
//                   reviewers, subscribers, transitions
//
// Weighted distribution (mirrors typical TUI usage — read-heavy):
//   30%  workspace issue search (priority)
//   10%  workspace issue search (keyword, slow LIKE)
//    8%  project issue search
//   12%  issue detail (basic|common picked randomly)
//   12%  issue detail (relations|children|parent|transitions ...)
//   10%  member ops
//    8%  project ops
//   10%  workspace ops
//
// Login happens once in setup() and the JWT is reused (token TTL = 2h).
//
// Run locally (docker):
//   docker run --rm -i --network backend_default \
//     -v "$(pwd)/loadtest/k6:/scripts" \
//     -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
//     -e K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99),min,max,avg" \
//     grafana/k6:0.55.0 run \
//     -e BASE_URL=http://app:8080 -e VUS_MAX=20 -e DURATION=1m \
//     -e TESTID=run-006-baseline-v2 \
//     --out experimental-prometheus-rw \
//     /scripts/baseline.js
// ============================================================

import { sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { TESTID } from './lib/env.js';
import { login, authHeaders } from './lib/auth.js';
import {
  listWorkspaces, getWorkspace,
  listMembers, getMember,
  listProjects, getProject, listProjectMembers,
  searchWorkspaceIssues, searchWorkspaceIssuesByKeyword, searchProjectIssues,
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
    'http_req_duration{op:issue_search}':                ['p(95)<500',  'p(99)<1500'],
    'http_req_duration{op:project_issue_search}':        ['p(95)<400'],
    'http_req_duration{op:issue_search_keyword}':        ['p(95)<3000', 'p(99)<8000'],
    'http_req_duration{op:issue_basic}':                 ['p(95)<200'],
    'http_req_duration{op:issue_common}':                ['p(95)<200'],
    'http_req_duration{op:issue_relations}':             ['p(95)<300'],
  },
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

  // ----- 30% workspace issue search (priority) -----
  if      (r < 0.30) { searchWorkspaceIssues(h); }
  // ----- 10% workspace issue search (keyword) -----
  else if (r < 0.40) { searchWorkspaceIssuesByKeyword(h); }
  // -----  8% project issue search -----
  else if (r < 0.48) { searchProjectIssues(h); }
  // ----- 12% issue detail (top-level: basic / common) -----
  else if (r < 0.60) {
    const f = [issueBasic, issueCommon][randomIntBetween(0, 1)];
    f(h);
  }
  // ----- 12% issue detail (relations group) -----
  else if (r < 0.72) {
    const f = [issueRelations, issueChildren, issueParent, issueTransitions,
               issueReviewers, issueSubscribers][randomIntBetween(0, 5)];
    f(h);
  }
  // ----- 10% member ops -----
  else if (r < 0.82) {
    (Math.random() < 0.5 ? listMembers : getMember)(h);
  }
  // -----  8% project ops -----
  else if (r < 0.90) {
    const f = [listProjects, getProject, listProjectMembers][randomIntBetween(0, 2)];
    f(h);
  }
  // ----- 10% workspace ops -----
  else {
    (Math.random() < 0.5 ? listWorkspaces : getWorkspace)(h);
  }

  sleep(randomIntBetween(0, 1));   // think time 0~1s
}
