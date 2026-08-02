package logging

import (
	"fmt"
	"log"
	"log/slog"
	"os"
	"path/filepath"
)

const appName = "tissue"

// Setup points slog and the standard logger at <cache>/tissue/tissue.log
// and returns a cleanup func that closes the file.
func Setup() (func(), error) {
	base, err := os.UserCacheDir()
	if err != nil {
		return nil, fmt.Errorf("locate cache dir: %w", err)
	}
	dir := filepath.Join(base, appName)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return nil, fmt.Errorf("create cache dir %s: %w", dir, err)
	}

	f, err := os.OpenFile(
		filepath.Join(dir, "tissue.log"),
		os.O_APPEND|os.O_CREATE|os.O_WRONLY,
		0o600, // may hold sensitive context
	)
	if err != nil {
		return nil, fmt.Errorf("open log file: %w", err)
	}

	level := slog.LevelInfo
	if os.Getenv("TISSUE_DEBUG") != "" {
		level = slog.LevelDebug
	}
	slog.SetDefault(slog.New(slog.NewTextHandler(f, &slog.HandlerOptions{Level: level})))
	log.SetOutput(f)

	return func() { _ = f.Close() }, nil
}
