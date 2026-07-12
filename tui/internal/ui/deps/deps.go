// Package deps holds the immutable dependencies shared across screens.
package deps

import (
	"github.com/kiseki1011/TISSUE/tui/internal/auth"
	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// Deps is the set of dependencies wired once at startup and passed to screens.
type Deps struct {
	Server string

	Public *domain.AuthService

	Authed   *domain.AuthService
	Projects *domain.ProjectService
	Catalog  *domain.CatalogService

	Store     auth.TokenStore
	Transport *auth.Transport
	Config    *config.Config
	Styles    theme.Styles
	Glyphs    glyph.Set
}
