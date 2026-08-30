package project

import (
	"os"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"
	runewidth "github.com/mattn/go-runewidth"
)

// Mirrors the CLI's startup fix: under a CJK locale go-runewidth counts ambiguous runes as width 2,
// doubling bubblezone's coordinates. NewGlobal here so a test that marks a zone does not depend on
// another test having initialised the manager first.
func TestMain(m *testing.M) {
	runewidth.DefaultCondition.EastAsianWidth = false
	zone.NewGlobal()
	os.Exit(m.Run())
}
