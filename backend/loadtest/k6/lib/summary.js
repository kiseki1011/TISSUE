// ============================================================
// AI-GENERATED
// model: claude-opus-4-7
// NOT REVIEWED
// ============================================================
// Shared handleSummary helper.
// Emits HTML + JSON to /results (mounted from host) and a textSummary to stdout.
//
// Each stress script wires it like:
//   import { buildSummary } from '../lib/summary.js';
//   export function handleSummary(data) { return buildSummary(data, __ENV.TESTID); }
//
// HTML reporter is fetched from a pinned GitHub raw URL; k6 caches imports per
// run but does need network during script parse.

import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/2.4.0/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

export function buildSummary(data, testid) {
    const name = testid && testid.length > 0 ? testid : 'local';
    return {
        [`/results/${name}.html`]: htmlReport(data),
        [`/results/${name}.json`]: JSON.stringify(data, null, 2),
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}
