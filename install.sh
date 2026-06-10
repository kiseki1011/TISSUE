#!/usr/bin/env bash
#
# Tissue one-click installer (self-host / production)
#   - checks prerequisites
#   - creates .env from .env.example (auto-generates JWT_SECRET and the DB password)
#   - builds and starts the stack (compose.prod.yaml)
#   - waits until the app is healthy and prints the URL
#
# Does not overwrite existing .env
set -euo pipefail

# Run where compose.prod.yaml and .env lives
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

COMPOSE_FILE="compose.prod.yaml"
APP_CONTAINER="tissue-app"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || die "Docker is not installed. See https://docs.docker.com/get-docker/"
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required ('docker compose' command)."
command -v openssl >/dev/null 2>&1 || die "openssl is required to generate secrets."

gen_secret() { openssl rand -hex 32; }
get_kv() { grep -E "^$1=" .env | head -1 | cut -d= -f2- || true; }
set_kv() {
  local tmp; tmp="$(mktemp)"
  sed "s|^$1=.*|$1=$2|" .env > "$tmp" && mv "$tmp" .env
} # replace KEY=... in .env

if [ -f .env ]; then
  info ".env already exists, leaving it untouched."
else
  [ -f .env.example ] || die ".env.example not found"
  cp .env.example .env
  chmod 600 .env
  set_kv JWT_SECRET "$(gen_secret)"
  set_kv POSTGRES_PASSWORD "$(gen_secret)"
  info "Created .env with generated JWT_SECRET and POSTGRES_PASSWORD."
  warn "Before exposing publicly, review .env: TISSUE_BASE_URL, CORS_ALLOWED_ORIGINS, and SMTP (MAIL_*)."
fi

info "Building and starting Tissue..."
docker compose -f "$COMPOSE_FILE" up -d --build

info "Waiting for Tissue to become healthy..."
ok=0
for _ in $(seq 1 60); do
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$APP_CONTAINER" 2>/dev/null || echo missing)"
  case "$status" in
    healthy)   ok=1; break ;;
    unhealthy) die "Tissue reported unhealthy. Logs: docker compose -f $COMPOSE_FILE logs app" ;;
  esac
  sleep 5
done
[ "$ok" = 1 ] || die "Timed out waiting for health. Logs: docker compose -f $COMPOSE_FILE logs app"

base_url="$(get_kv TISSUE_BASE_URL)"
info "Tissue is running at: ${base_url:-http://localhost:8080}"
echo "  - Initial account to sign up becomes a super admin."
echo "  - Configure SMTP (MAIL_* in .env) to enable email verification, then re-run:"
echo "      docker compose -f $COMPOSE_FILE up -d"
