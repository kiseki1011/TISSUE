.PHONY: build run fmt lint test tidy gen

# Regenerate the API client + OpenAPI docs
gen:
	go run ./internal/tools/specprep ../docs/api-docs.json api/openapi.json
	go tool oapi-codegen -config oapi-codegen.yaml api/openapi.json

build:
	go build -o bin/tissue ./cmd/tissue

run:
	go run ./cmd/tissue

fmt:
	gofumpt -w . && go mod tidy

lint:
	go vet ./... && golangci-lint run && govulncheck ./...

test:
	go test -race ./...

tidy:
	go mod tidy
