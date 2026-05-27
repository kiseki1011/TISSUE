// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Keyword pools for issue search load tests.
//
// SINGLE: Each issue's title/content is composed
//         from this exact pool, so every word here is guaranteed to match
//         a large slice of rows. Worst-case selectivity for FTS — useful
//         for measuring the floor, not the SLO target.
//
// MULTI : Each pair was picked so both words occur often enough to produce
//         a non-empty result set, but the AND-intersection is small (~1-2% of rows)
//         this is the realistic case where a search index can actually
//         prune the candidate set before ranking.
//
// Keep both pools >= 50 entries: at 100 VU × 60 s the test issues ~6k
// search calls, so a 20-keyword pool means each phrase repeats ~300x
// and shared_buffers + plan cache make the second hit unrealistically
// fast. 50+ entries keep cache-hit ratios closer to production.
//
// All entries are >= 3 chars (DB FTS often drops 1-2 char tokens).

import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ------------------------------------------------------------
// Single-word keywords (80, == seed vocab)
// ------------------------------------------------------------
export const SINGLE_KEYWORDS = [
  // auth / session
  'login','signup','password','token','session',
  // data store
  'cache','redis','postgres','sqlite','mysql',
  // messaging
  'queue','kafka','rabbit','stream','batch',
  // release
  'deploy','rollback','canary','staging','prod',
  // container / infra
  'docker','kubernetes','pod','helm','cluster',
  // network
  'network','firewall','router','gateway','proxy',
  // observability
  'metric','trace','span','log','alert',
  // dashboards
  'dashboard','panel','chart','widget','report',
  // notifications
  'webhook','event','notification','email','slack',
  // auth providers
  'oauth','jwt','saml','ldap','permission',
  // data lifecycle
  'export','import','migration','schema','backup',
  'snapshot','restore','archive','cleanup','retention',
  // listing
  'search','filter','sort','paginate','index',
  // attachments
  'upload','download','attachment','preview','thumbnail',
  // agile
  'sprint','backlog','epic','story','bug',
  // collab
  'review','comment','mention','reaction','edit',
];

// ------------------------------------------------------------
// Two-word phrases (~70) — selectivity ~1-2%
// ------------------------------------------------------------
export const MULTI_KEYWORDS = [
  // auth
  'login password','login session','signup token','password token','session jwt',
  'oauth jwt','oauth saml','jwt ldap','saml permission','ldap permission',
  // data store
  'cache redis','redis postgres','postgres mysql','postgres sqlite','mysql migration',
  'migration schema','schema backup','backup restore','snapshot restore','snapshot archive',
  // messaging
  'queue kafka','kafka stream','rabbit queue','stream batch','batch queue',
  // notifications
  'event webhook','webhook notification','notification email','email slack','event slack',
  // release
  'deploy docker','deploy rollback','rollback canary','canary staging','staging prod',
  // container / infra
  'docker kubernetes','kubernetes pod','pod cluster','helm chart','cluster network',
  // network
  'network firewall','firewall router','router gateway','gateway proxy','proxy network',
  // observability
  'metric alert','metric dashboard','trace span','span log','log alert',
  'dashboard panel','chart widget','widget report','panel chart','alert notification',
  // listing
  'search filter','search index','filter sort','sort paginate','paginate index',
  // data lifecycle
  'export import','import schema','archive cleanup','cleanup retention',
  // attachments
  'upload download','upload attachment','attachment preview','preview thumbnail','download attachment',
  // agile
  'sprint backlog','sprint review','epic story','story bug','bug review',
  // collab
  'review comment','comment mention','mention reaction','comment edit','reaction edit',
];

export function pickSingleKeyword() {
  return SINGLE_KEYWORDS[randomIntBetween(0, SINGLE_KEYWORDS.length - 1)];
}

export function pickMultiKeyword() {
  return MULTI_KEYWORDS[randomIntBetween(0, MULTI_KEYWORDS.length - 1)];
}
