package cli

import (
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
	runewidth "github.com/mattn/go-runewidth"
	"github.com/spf13/cobra"

	"github.com/kiseki1011/TISSUE/tui/internal/auth"
	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/logging"
	"github.com/kiseki1011/TISSUE/tui/internal/ui"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

var (
	serverFlag string
	themeFlag  string
	iconsFlag  string
)

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
	root.PersistentFlags().StringVar(&themeFlag, "theme", "",
		"color theme: tokyo-night, dracula, gruvbox, solarized-light")
	root.PersistentFlags().StringVar(&iconsFlag, "icons", "",
		"glyph set: auto, nerd, unicode")
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

	d, err := buildDeps(server, cfg)
	if err != nil {
		return fmt.Errorf("build dependencies: %w", err)
	}

	// Under a CJK locale go-runewidth counts ambiguous-width runes (box-drawing) as width 2, but lipgloss
	// and the terminal render them width 1, so uncorrected bubblezone X doubles and mouse hits miss. Force width 1.
	runewidth.DefaultCondition.EastAsianWidth = false

	zone.NewGlobal()
	defer zone.Close()

	if _, err := tea.NewProgram(ui.New(d)).Run(); err != nil {
		return fmt.Errorf("run tui: %w", err)
	}
	return nil
}

// buildDeps assembles the API clients, token store, and refreshing transport.
// The public client stays unauthenticated so token refresh cannot recurse back through the transport.
func buildDeps(server string, cfg *config.Config) (deps.Deps, error) {
	store, err := auth.NewTokenStore()
	if err != nil {
		return deps.Deps{}, fmt.Errorf("open token store: %w", err)
	}

	publicAPI, err := client.NewClientWithResponses(server)
	if err != nil {
		return deps.Deps{}, fmt.Errorf("build public client: %w", err)
	}
	public := domain.NewAuthService(publicAPI)

	transport := auth.NewTransport(nil, public.Refresh, func(tokens domain.TokenPair) {
		if err := store.Save(server, tokens); err != nil {
			slog.Warn("save refreshed tokens", "err", err)
		}
	})

	authedAPI, err := client.NewClientWithResponses(server,
		client.WithHTTPClient(&http.Client{Transport: transport}))
	if err != nil {
		return deps.Deps{}, fmt.Errorf("build authed client: %w", err)
	}

	return deps.Deps{
		Server:    server,
		Public:    public,
		Authed:    domain.NewAuthService(authedAPI),
		Projects:  domain.NewProjectService(authedAPI),
		Catalog:   domain.NewCatalogService(authedAPI),
		Store:     store,
		Transport: transport,
		Config:    cfg,
		Styles:    theme.New(theme.ByName(resolveTheme(cfg))),
		Glyphs:    glyph.New(glyph.ParseMode(resolveIcons(cfg))),
	}, nil
}

func resolveServer(cfg *config.Config) string {
	if serverFlag != "" {
		return serverFlag
	}
	return cfg.ServerURL
}

func resolveTheme(cfg *config.Config) string {
	if themeFlag != "" {
		return themeFlag
	}
	return cfg.Theme
}

func resolveIcons(cfg *config.Config) string {
	if iconsFlag != "" {
		return iconsFlag
	}
	return cfg.Icons
}
