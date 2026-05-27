// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Tissue baseline load test scenario  (mixed read 80% / write 20%)
//
// Read AND write target WS0001 (1M+ issue seed). Writes are
// later removed by cleanup.sql (rows where created_by = loadadmin).
//
// Endpoints covered:
//   READ (80%)
//     issue search:      cursor - single-word 20%, two-word 10%        (30%)
//     issue detail:      basic|common, parent|children|relations,
//                        reviewers|subscribers|transitions             (20%)
//     wiki:              roots, get (cached id), search                (10%)
//     workspace ops:     list, get                                     (3%)
//     project ops:       list, get, list-members, tags, sprints,
//                        issue-types                                   (9%)
//     member ops:        list, get                                     (4%)
//     comment list                                                     (4%)
//   WRITE (20%)
//     POST comment                                                     (5%)
//     PATCH issue (priority)                                           (3%)
//     PATCH storypoint                                                 (2%)
//     POST/DELETE subscribe (toggle)                                   (2%)
//     POST issue (new)                                                 (1%)
//     POST wiki                                                        (3%)
//     PATCH wiki content                                               (4%)
//
// setup() does:
//   1. login (token cached for the whole run)
//   2. discoverIssueTypes - projectKey + issueTypeId per project (createIssue)
//   3. discoverReadWikis(50) - cached wiki ids for GET/PATCH targets
//
// Run inside docker (see loadtest/k6/run.sh):
//   ./loadtest/k6/run.sh baseline -d 1m -u 100 -p -i 1000 -b http://{host}:8080
// ============================================================

import { sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { TESTID } from './lib/env.js';
import { login, authHeaders } from './lib/auth.js';
import { buildSummary } from './lib/summary.js';
import { discoverIssueTypes, discoverReadWikis } from './lib/discover.js';
import {
  // Read
  listWorkspaces, getWorkspace,
  listMembers, getMember,
  listProjects, getProject, listProjectMembers,
  listProjectTags, listProjectSprints, listProjectIssueTypes,
  searchIssuesSingle, searchIssuesMulti,
  issueBasic, issueCommon, issueParent, issueChildren,
  issueRelations, issueReviewers, issueSubscribers, issueTransitions,
  listIssueComments,
  listWikiRoots, searchWiki, getWikiDocument,
  // Write
  createComment, updateIssuePriority, updateStoryPoint, toggleSubscribeIssue,
  createIssue, createWiki, updateWikiContent,
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
    'http_req_failed': ['rate<0.01'],
    'errors':          ['rate<0.01'],
    // Read SLOs
    'http_req_duration{op:list_workspaces}':              ['p(95)<300'],
    'http_req_duration{op:get_workspace}':                ['p(95)<200'],
    'http_req_duration{op:list_members}':                 ['p(95)<400'],
    'http_req_duration{op:get_member}':                   ['p(95)<200'],
    'http_req_duration{op:list_projects}':                ['p(95)<300'],
    'http_req_duration{op:get_project}':                  ['p(95)<200'],
    'http_req_duration{op:list_project_members}':         ['p(95)<400'],
    'http_req_duration{op:issue_search_multi}':           ['p(95)<200',  'p(99)<400'],
    'http_req_duration{op:issue_search_single}':          ['p(95)<400',  'p(99)<500'],
    'http_req_duration{op:issue_basic}':                  ['p(95)<200'],
    'http_req_duration{op:issue_common}':                 ['p(95)<300'],
    'http_req_duration{op:issue_relations}':              ['p(95)<300'],
    // Write SLOs
    'http_req_duration{op:comment_create}':               ['p(95)<500'],
    'http_req_duration{op:issue_update}':                 ['p(95)<300'],
    'http_req_duration{op:wiki_create}':                  ['p(95)<500'],
    'http_req_duration{op:wiki_update}':                  ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { testid: TESTID },
};

export function setup() {
  const token = login();
  const issueTypes = discoverIssueTypes(token);
  const wikiIds    = discoverReadWikis(token, 50);
  console.log(`✓ setup ok | testid=${TESTID} | issue-types=${issueTypes.length} | wikis=${wikiIds.length}`);
  return { token, issueTypes, wikiIds };
}

function pickIssueType(data) {
  return data.issueTypes[randomIntBetween(0, data.issueTypes.length - 1)];
}
function pickWikiId(data) {
  return data.wikiIds[randomIntBetween(0, data.wikiIds.length - 1)];
}

export default function (data) {
  const h = authHeaders(data.token);
  const r = Math.random();

  // -------- WRITE 20% --------
  if      (r < 0.05) { createComment(h); }
  else if (r < 0.08) { updateIssuePriority(h); }
  else if (r < 0.10) { updateStoryPoint(h); }
  else if (r < 0.12) { toggleSubscribeIssue(h); }
  else if (r < 0.13) {
    const t = pickIssueType(data);
    createIssue(h, t.projectKey, t.id);
  }
  else if (r < 0.16) { createWiki(h); }
  else if (r < 0.20) { updateWikiContent(h, pickWikiId(data)); }

  // -------- READ search (30%) — single 20% + multi 10% --------
  else if (r < 0.40) { searchIssuesSingle(h); }
  else if (r < 0.50) { searchIssuesMulti(h); }

  // -------- READ issue detail (20%) --------
  else if (r < 0.58) {
    [issueBasic, issueCommon][randomIntBetween(0, 1)](h);
  }
  else if (r < 0.66) {
    [issueParent, issueChildren, issueRelations][randomIntBetween(0, 2)](h);
  }
  else if (r < 0.70) {
    [issueReviewers, issueSubscribers, issueTransitions][randomIntBetween(0, 2)](h);
  }

  // -------- READ wiki (10%) --------
  else if (r < 0.75) { listWikiRoots(h); }
  else if (r < 0.78) { getWikiDocument(h, pickWikiId(data)); }
  else if (r < 0.80) { searchWiki(h); }

  // -------- READ member / project / comment / tag / sprint / workspace (20%) --------
  else if (r < 0.84) {
    (Math.random() < 0.5 ? listMembers : getMember)(h);
  }
  else if (r < 0.89) {
    [listProjects, getProject, listProjectMembers][randomIntBetween(0, 2)](h);
  }
  else if (r < 0.93) { listIssueComments(h); }
  else if (r < 0.97) {
    [listProjectTags, listProjectSprints, listProjectIssueTypes][randomIntBetween(0, 2)](h);
  }
  else {
    (Math.random() < 0.5 ? listWorkspaces : getWorkspace)(h);
  }

  sleep(randomIntBetween(0, 1));
}

export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
