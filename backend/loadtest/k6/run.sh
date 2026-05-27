#!/usr/bin/env bash
# Wrapper for running k6 scenarios inside a docker container.
# Run from the backend/ directory (where compose.loadtest.yaml lives).
#
# Usage:
#   ./loadtest/k6/run.sh <scenario> [options]
#
# Scenarios:
#   baseline                 → loadtest/k6/baseline.js
#   <name>                   → loadtest/k6/stress/<name>.js
#                              example: issue-common, issue-detail
#
# Options (flags take precedence over env vars; env vars take precedence over defaults):
#   -d, --duration <time>     k6 run duration       (env: DURATION,        default: 1m)
#   -u, --vus <n>             max virtual users     (env: VUS_MAX,         default: 20)
#   -t, --testid <name>       run id / report name  (env: TESTID,          default: timestamp)
#   -b, --base-url <url>      app base URL          (env: BASE_URL,        default: http://app:8080)
#   -n, --network <name>      docker network        (env: NETWORK,         default: backend_default)
#   -i, --issues-per-proj <n> seed shape            (env: ISSUES_PER_PROJ, default: 1000)
#   -p, --prometheus          push metrics to Prometheus via remote-write
#                             (uses K6_PROMETHEUS_RW_SERVER_URL, default: http://prometheus:9090/api/v1/write)
#   -c, --cleanup             after the run, run cleanup.sql to delete rows k6 inserted
#                             (env: DB_CONTAINER, default: tissue-loadtest-db)
#   -h, --help                show this help
#
# Anything after `--` is passed through to k6 unchanged.
#
# Examples:
#   ./loadtest/k6/run.sh baseline
#   ./loadtest/k6/run.sh baseline -d 3m -u 50
#   ./loadtest/k6/run.sh baseline -t baseline-after-fix -p -c
#   ./loadtest/k6/run.sh issue-common -d 30s -u 10
#   ./loadtest/k6/run.sh issue-common -p -- --verbose

set -euo pipefail

print_help() { sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'; }

if [[ $# -lt 1 ]]; then
  print_help
  exit 2
fi

SCENARIO="$1"; shift
if [[ "$SCENARIO" == "-h" || "$SCENARIO" == "--help" ]]; then
  print_help; exit 0
fi

DURATION="${DURATION:-1m}"
VUS_MAX="${VUS_MAX:-20}"
TESTID=""
BASE_URL="${BASE_URL:-http://app:8080}"
NETWORK="${NETWORK:-backend_default}"
ISSUES_PER_PROJ="${ISSUES_PER_PROJ:-1000}"
USE_PROMETHEUS=0
RUN_CLEANUP=0
DB_CONTAINER="${DB_CONTAINER:-tissue-loadtest-db}"
K6_PASSTHROUGH=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -d|--duration)        DURATION="$2"; shift 2;;
    -u|--vus)             VUS_MAX="$2"; shift 2;;
    -t|--testid)          TESTID="$2"; shift 2;;
    -b|--base-url)        BASE_URL="$2"; shift 2;;
    -n|--network)         NETWORK="$2"; shift 2;;
    -i|--issues-per-proj) ISSUES_PER_PROJ="$2"; shift 2;;
    -p|--prometheus)      USE_PROMETHEUS=1; shift;;
    -c|--cleanup)         RUN_CLEANUP=1; shift;;
    -h|--help)            print_help; exit 0;;
    --)                   shift; K6_PASSTHROUGH=("$@"); break;;
    *) echo "unknown option: $1" >&2; print_help; exit 2;;
  esac
done

TESTID="${TESTID:-$(date +%Y%m%d-%H%M%S)-${SCENARIO}}"

if [[ "$SCENARIO" == "baseline" ]]; then
  SCRIPT=/scripts/baseline.js
else
  SCRIPT=/scripts/stress/${SCENARIO}.js
fi

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

K6_OUT_ARGS=()
PROM_URL=""
if [[ "$USE_PROMETHEUS" -eq 1 ]]; then
  PROM_URL="${K6_PROMETHEUS_RW_SERVER_URL:-http://prometheus:9090/api/v1/write}"
  K6_OUT_ARGS+=("--out" "experimental-prometheus-rw")
fi

cat <<EOF
→ scenario        : ${SCENARIO}
→ script          : ${SCRIPT}
→ testid          : ${TESTID}
→ duration        : ${DURATION}
→ vus_max         : ${VUS_MAX}
→ base_url        : ${BASE_URL}
→ network         : ${NETWORK}
→ issues_per_proj : ${ISSUES_PER_PROJ}
→ prometheus      : $([[ "$USE_PROMETHEUS" -eq 1 ]] && echo "${PROM_URL}" || echo "(disabled)")
→ cleanup         : $([[ "$RUN_CLEANUP" -eq 1 ]] && echo "yes (after run, via ${DB_CONTAINER})" || echo "(disabled)")
EOF
echo

docker run --rm -i \
  --network "${NETWORK}" \
  -v "${REPO_ROOT}/loadtest/k6:/scripts" \
  -v "${REPO_ROOT}/loadtest/results:/results" \
  -e BASE_URL="${BASE_URL}" \
  -e TESTID="${TESTID}" \
  -e VUS_MAX="${VUS_MAX}" \
  -e DURATION="${DURATION}" \
  -e ISSUES_PER_PROJ="${ISSUES_PER_PROJ}" \
  -e IDENTIFIER="${IDENTIFIER:-loadadmin@loadtest.local}" \
  -e PASSWORD="${PASSWORD:-Loadtest1!}" \
  -e WORKSPACE_KEY="${WORKSPACE_KEY:-WS0001}" \
  -e K6_PROMETHEUS_RW_SERVER_URL="${PROM_URL}" \
  -e K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99),min,max,avg" \
  grafana/k6:0.55.0 run ${K6_OUT_ARGS[@]+"${K6_OUT_ARGS[@]}"} "${SCRIPT}" ${K6_PASSTHROUGH[@]+"${K6_PASSTHROUGH[@]}"}

if [[ "$RUN_CLEANUP" -eq 1 ]]; then
  echo
  echo "→ running cleanup.sql in ${DB_CONTAINER} ..."
  docker exec -i "${DB_CONTAINER}" psql -U tissue -d tissue -f /seed/cleanup.sql
fi
