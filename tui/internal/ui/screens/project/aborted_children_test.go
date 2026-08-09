package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Aborted children sink below the active ones (so the active work reads first) and render struck-through.
func TestAbortedChildrenSinkAndMuted(t *testing.T) {
	det := sampleDetail()
	det.Children = []domain.IssueRef{
		{Key: "ENG-2", TypeName: "Task", StateLabel: "Done", StateCategory: "COMPLETED"},
		{Key: "ENG-3", TypeName: "Task", StateLabel: "Cancelled", StateCategory: "ABORTED"},
		{Key: "ENG-4", TypeName: "Task", StateLabel: "Doing", StateCategory: "ACTIVE"},
	}
	m := editReady(t, det)
	raw := m.detailBody(80)
	body := plain(raw)

	iActive := strings.Index(body, "ENG-4")
	iAborted := strings.Index(body, "ENG-3")
	if iActive < 0 || iAborted < 0 {
		t.Fatalf("children not rendered: active=%d aborted=%d\n%s", iActive, iAborted, body)
	}
	if iAborted < iActive {
		t.Errorf("an aborted child should sink below the active ones, got aborted@%d before active@%d", iAborted, iActive)
	}

	// the aborted key is muted + struck through: build the same style and match its opening SGR (lipgloss
	// combines strikethrough and foreground into one escape, so the reference must set both)
	ref := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Strikethrough(true).Render("x")
	code := ref[:strings.IndexByte(ref, 'x')] // the opening escape, before the content
	if !strings.Contains(raw, code) {
		t.Error("an aborted child key should be muted and struck through")
	}
}
