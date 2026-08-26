package domain

import (
	"regexp"
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// fixedOffset matches the "+09:00" fallback form.
var fixedOffset = regexp.MustCompile(`^[+-]\d{2}:\d{2}$`)

// serverParseable mirrors the backend's ZoneId.of: an IANA id or an explicit offset. Anything else
// silently drops the server back to UTC.
func serverParseable(t *testing.T, id string) {
	t.Helper()
	if id == "" || id == "Local" {
		t.Fatalf("%q would leave the server on UTC", id)
	}
	if fixedOffset.MatchString(id) {
		return
	}
	if _, err := time.LoadLocation(id); err != nil {
		t.Fatalf("%q is neither an IANA id nor an offset: %v", id, err)
	}
}

func TestResolveLocalZonePrefersTZ(t *testing.T) {
	t.Setenv("TZ", "America/New_York")
	if got := resolveLocalZoneID(); got != "America/New_York" {
		t.Errorf("an explicit TZ should be used verbatim, got %q", got)
	}
}

// A forwarded bad TZ would leave the server bucketing on UTC, the exact bug this parameter fixes.
func TestResolveLocalZoneIgnoresUnloadableTZ(t *testing.T) {
	t.Setenv("TZ", "Not/AZone")
	got := resolveLocalZoneID()
	if got == "Not/AZone" {
		t.Error("an unloadable TZ must not be forwarded to the server")
	}
	serverParseable(t, got)
}

// No TZ is the common case: Go reports only "Local", so resolution must still produce something usable.
func TestResolveLocalZoneWithoutTZ(t *testing.T) {
	t.Setenv("TZ", "")
	serverParseable(t, resolveLocalZoneID())
}

func TestLocalZoneIDIsStable(t *testing.T) {
	first := LocalZoneID()
	serverParseable(t, first)
	if second := LocalZoneID(); second != first {
		t.Errorf("the zone should not change between calls: %q then %q", first, second)
	}
}

// The request builders are generated code, so check the zone actually reaches the query string.
func TestStatsRequestsCarryTheZone(t *testing.T) {
	zone := LocalZoneID()
	days := int32(126)
	window := "month"

	contrib, err := client.NewGetProjectContributionsRequest(
		"http://x", "PROJ", &client.GetProjectContributionsParams{MemberId: 1, Days: &days, ZoneId: &zone})
	if err != nil {
		t.Fatalf("building the contributions request: %v", err)
	}
	if q := contrib.URL.Query().Get("zoneId"); q != zone {
		t.Errorf("contributions request lost the zone: query=%q want %q (url=%s)", q, zone, contrib.URL)
	}

	flow, err := client.NewGetProjectFlowStatsRequest(
		"http://x", "PROJ", &client.GetProjectFlowStatsParams{Window: &window, ZoneId: &zone})
	if err != nil {
		t.Fatalf("building the flow request: %v", err)
	}
	if q := flow.URL.Query().Get("zoneId"); q != zone {
		t.Errorf("flow request lost the zone: query=%q want %q (url=%s)", q, zone, flow.URL)
	}
	if !strings.Contains(flow.URL.RawQuery, "window=") {
		t.Errorf("the existing params should survive alongside it: %s", flow.URL.RawQuery)
	}
}
