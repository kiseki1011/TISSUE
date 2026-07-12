.PHONY: build run fmt lint test tidy

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

# TODO: `gen` (spec preprocessing + oapi-codegen)
