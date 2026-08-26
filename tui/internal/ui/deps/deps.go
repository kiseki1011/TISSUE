// Package deps holds the immutable dependencies shared across screens.
package deps

import (
	"github.com/kiseki1011/TISSUE/tui/internal/auth"
	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

type Deps struct {
	Server string

	Public *domain.AuthService

	Authed   *domain.AuthService
	Projects *domain.ProjectService
	Catalog  *domain.CatalogService
	Agents   *domain.AgentService
	Issues   *domain.IssueService
	Sprints  *domain.SprintService

	Notifications *domain.NotificationService

	Store     auth.TokenStore
	Transport *auth.Transport
	Config    *config.Config
	Styles    theme.Styles
	Glyphs    glyph.Set
	Icons     string // active glyph mode (auto/nerd/unicode), for the Options picker
	Mouse     bool   // mouse capture is on. off hides click-only affordances (pens, + buttons)
}
