#!/bin/bash
set -e

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

info() {
    echo -e "${BLUE}→${NC} $1"
}
success() {
    echo -e "${GREEN}✓${NC} $1"
}
error() {
    echo -e "${RED}✗${NC} $1"
}

trap 'error "Failed at line $LINENO. Aborting."' ERR

# Move to client/tui directory
cd "$(dirname "$0")/.."

echo ""
info "Starting API client regeneration..."
echo ""

info "[1/4] Extracting OpenAPI spec from backend..."
(cd ../../backend && ./gradlew :tissue-bootstrap:generateOpenApiDocs)
success "Spec extracted to docs/api-docs.json"
echo ""

info "[2/4] Removing old generated code..."
rm -rf src/tissue/api/generated
success "Old generated code removed"
echo ""

info "[3/4] Generating new Python client..."
openapi-generator generate \
    -i ../../docs/api-docs.json \
    -g python \
    -o src \
    --additional-properties="library=httpx,packageName=tissue.api.generated,generateSourceCodeOnly=true,pythonVersion=3.14"
success "Client generated at src/tissue/api/generated/"
echo ""

info "[4/4] Cleaning up generator metadata and boilerplate..."
rm -rf src/.openapi-generator
rm -rf src/tissue/api/generated/docs
rm -rf src/tissue/api/generated/test
rm -f src/tissue/api/generated_README.md
success "Cleanup complete"
echo ""

success "Done! API client is up to date."
