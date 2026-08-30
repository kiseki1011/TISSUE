.PHONY: help check build run fmt lint test tidy gen
.DEFAULT_GOAL := help

help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@awk '/^## /{d=substr($$0,4);next} /^[a-zA-Z0-9_-]+:/{if(d){t=$$0;sub(/:.*/,"",t);printf "  %-7s %s\n",t,d;d=""}next} {d=""}' $(MAKEFILE_LIST)

## Run everything CI runs. Rewrites formatting, so use it before committing
check: fmt lint test

## Regenerate the API client from the backend's OpenAPI spec
gen:
	go run ./internal/tools/specprep ../docs/api-docs.json api/openapi.json
	go tool oapi-codegen -config oapi-codegen.yaml api/openapi.json

## Compile the binary to bin/tissue
build:
	go build -o bin/tissue ./cmd/tissue

## Run the TUI from source
run:
	go run ./cmd/tissue

## Rewrite formatting and tidy go.mod
fmt:
	gofumpt -w . && go mod tidy

## Vet, lint, and scan dependencies for known vulnerabilities
lint:
	go vet ./... && golangci-lint run && govulncheck ./...

## Run the tests with the race detector, in shuffled order
test:
	go test -race -shuffle=on ./...

## Drop unused dependencies from go.mod
tidy:
	go mod tidy
