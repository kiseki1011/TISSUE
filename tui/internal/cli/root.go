package cli

import (
	"errors"
	"fmt"
	"os"

	tea "charm.land/bubbletea/v2"
	"github.com/spf13/cobra"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/logging"
	"github.com/kiseki1011/TISSUE/tui/internal/ui"
)

var serverFlag string

// Execute runs the command tree and returns a process exit code.
func Execute() int {
	if err := newRootCmd().Execute(); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		return 1
	}
	return 0
}

func newRootCmd() *cobra.Command {
	root := &cobra.Command{
		Use:   "tissue",
		Short: "Tissue project-management TUI",
		// Silence Cobra's usage/error dump so a runtime failure prints only our error line.
		SilenceUsage:  true,
		SilenceErrors: true,
		RunE:          runTUI,
	}
	root.PersistentFlags().StringVarP(&serverFlag, "server", "c", "",
		"server base URL (e.g. https://tissue.example.com)")
	root.AddCommand(newVersionCmd())
	return root
}

func runTUI(_ *cobra.Command, _ []string) error {
	cleanup, err := logging.Setup()
	if err != nil {
		return fmt.Errorf("set up logging: %w", err)
	}
	defer cleanup()

	cfg, err := config.Load()
	if err != nil {
		return fmt.Errorf("load config: %w", err)
	}

	server := resolveServer(cfg)
	if server == "" {
		return errors.New("no server configured; connect with: tissue -c <url>")
	}

	if _, err := tea.NewProgram(ui.New(server)).Run(); err != nil {
		return fmt.Errorf("run tui: %w", err)
	}
	return nil
}

func resolveServer(cfg *config.Config) string {
	if serverFlag != "" {
		return serverFlag
	}
	return cfg.ServerURL
}
