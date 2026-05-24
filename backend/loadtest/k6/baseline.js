// ============================================================
// Tissue baseline load test scenario
//
// What this exercises (read-heavy, typical TUI usage):
//   60% — workspace-scoped issue search (paged, with priority filter)
//   20% — issue detail (basic/common/relations)
//   15% — workspace member list
//    5% — workspace + project metadata
//
// Login happens once in setup() and the JWT is reused (token TTL = 2h).
// Each VU then issues N requests/sec until VUs ramp down.
//
// Run locally:
//   k6 run loadtest/k6/baseline.js
//
// Override defaults:
//   k6 run -e BASE_URL=http://localhost:8080 \
//          -e WORKSPACE_KEY=WS0001 \
//          -e VUS_MAX=200 \
//          -e DURATION=5m \
//          loadtest/k6/baseline.js
//
// Push metrics to Prometheus (Grafana visualization):
//   K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
//   k6 run --out experimental-prometheus-rw loadtest/k6/baseline.js
// ============================================================

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// env
const BASE             = __ENV.BASE_URL       || 'http://localhost:8080';
const IDENTIFIER       = __ENV.IDENTIFIER     || 'loadadmin@loadtest.local';
const PASSWORD         = __ENV.PASSWORD       || 'Loadtest1!';
const WORKSPACE_KEY    = __ENV.WORKSPACE_KEY  || 'WS0001';

// Seeded data shape: WS0001 → P0001..P0010, each with 10_000 issues (P0001-1..P0001-10000)
const PROJECT_KEYS     = ['P0001','P0002','P0003','P0004','P0005','P0006','P0007','P0008','P0009','P0010'];
const ISSUES_PER_PROJ  = parseInt(__ENV.ISSUES_PER_PROJ || '10000');

// custom metrics
const errorRate        = new Rate('errors');
const searchLatency    = new Trend('biz_issue_search_ms', true);
const detailLatency    = new Trend('biz_issue_detail_ms', true);

// ramping schedule
const VUS_MAX  = parseInt(__ENV.VUS_MAX  || '100');
const DURATION = __ENV.DURATION || '3m';

export const options = {
  scenarios: {
    ramping_baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s',     target: Math.ceil(VUS_MAX * 0.2) },   // warm-up
        { duration: '1m',      target: Math.ceil(VUS_MAX * 0.5) },   // ramp
        { duration: DURATION,  target: VUS_MAX               },      // sustained
        { duration: '30s',     target: 0                     },      // ramp down
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: {
    // Overall: <1% of HTTP requests may fail
    'http_req_failed':                           ['rate<0.01'],
    'errors':                                    ['rate<0.01'],
    // Read-path SLOs (matches /actuator metrics SLO buckets: 100ms,300ms,1s)
    'http_req_duration{op:list_workspaces}':     ['p(95)<300'],
    'http_req_duration{op:issue_search}':        ['p(95)<500', 'p(99)<1500'],
    'http_req_duration{op:issue_detail}':        ['p(95)<200'],
    'http_req_duration{op:list_members}':        ['p(95)<400'],
  },
  // Tag everything with this so Grafana queries can filter by run
  tags: { testid: __ENV.TESTID || 'baseline-local' },
};

// setup() — runs once before VUs start
export function setup() {
  const res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ identifier: IDENTIFIER, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { op: 'login' } }
  );
  const ok = check(res, { 'login 200': r => r.status === 200 });
  if (!ok) throw new Error(`login failed: ${res.status} ${res.body}`);

  const token = res.json('accessToken');
  console.log(`✓ login ok (token len=${token.length})`);
  return { token };
}

// default VU function
export default function (data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
    'Content-Type':  'application/json',
  };

  // weighted-random pick: 60/20/15/5
  const r = Math.random();

  if (r < 0.60) {
    issueSearch(headers);
  } else if (r < 0.80) {
    issueDetail(headers);
  } else if (r < 0.95) {
    listMembers(headers);
  } else {
    workspaceMeta(headers);
  }

  sleep(randomIntBetween(0, 1));   // think time 0~1s
}

function issueSearch(headers) {
  // Randomise the query to avoid cache-hit-only behaviour
  const priorities = ['P0,P1', 'P2,P3', 'P0,P1,P2'][randomIntBetween(0, 2)];
  const page       = randomIntBetween(0, 50);

  const res = http.get(
    `${BASE}/api/v1/workspaces/${WORKSPACE_KEY}/issues?priorities=${priorities}&page=${page}&size=20`,
    { headers, tags: { op: 'issue_search' } }
  );
  check(res, { 'search 200': r => r.status === 200 }) || errorRate.add(1);
  searchLatency.add(res.timings.duration);
}

function issueDetail(headers) {
  const projectKey = PROJECT_KEYS[randomIntBetween(0, PROJECT_KEYS.length - 1)];
  const n          = randomIntBetween(1, ISSUES_PER_PROJ);
  const issueKey   = `${projectKey}-${n}`;

  // Hit two of the detail endpoints (typical TUI behaviour)
  const res = http.get(
    `${BASE}/api/v1/workspaces/${WORKSPACE_KEY}/issues/${issueKey}/basic`,
    { headers, tags: { op: 'issue_detail' } }
  );
  check(res, { 'detail 200': r => r.status === 200 }) || errorRate.add(1);
  detailLatency.add(res.timings.duration);
}

function listMembers(headers) {
  const res = http.get(
    `${BASE}/api/v1/workspaces/${WORKSPACE_KEY}/members?page=0&size=20`,
    { headers, tags: { op: 'list_members' } }
  );
  check(res, { 'members 200': r => r.status === 200 }) || errorRate.add(1);
}

function workspaceMeta(headers) {
  group('workspace+projects', () => {
    let res = http.get(`${BASE}/api/v1/workspaces/me`,
      { headers, tags: { op: 'list_workspaces' } });
    check(res, { 'workspaces 200': r => r.status === 200 }) || errorRate.add(1);

    res = http.get(`${BASE}/api/v1/workspaces/${WORKSPACE_KEY}/projects?page=0&size=20`,
      { headers, tags: { op: 'list_projects' } });
    check(res, { 'projects 200': r => r.status === 200 }) || errorRate.add(1);
  });
}
