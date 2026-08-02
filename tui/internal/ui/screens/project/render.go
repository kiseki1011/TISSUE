package project

import (
	"fmt"
	"image/color"
	"strconv"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const zoneBack = "project.back"

func issueRowZone(i int) string { return "project.row." + strconv.Itoa(i) }

// column widths for the issue table (title flexes to fill the rest).
const (
	colKey   = 11
	colType  = 12
	colState = 14
	colPri   = 3
	colAsg   = 1
	rowH     = 1 // issue rows are a single line
)

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	s := m.deps.Styles
	if m.width < minWidth || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, s.Muted.Render("Terminal too small"))
	}

	header := m.header()
	listH := max(1, m.height-2) // header + one blank row
	list := m.listPanel(listH)
	return lipgloss.JoinVertical(lipgloss.Left, header, "", list)
}

// header is the breadcrumb. The title takes the leftover width and is truncated (never padded).
func (m Model) header() string {
	t := m.deps.Styles.Theme
	back := lipgloss.NewStyle().Foreground(t.Accent).Render("← Projects")
	sep := lipgloss.NewStyle().Foreground(t.Muted).Render("  ·  ")
	keyPart := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(m.projectKey)
	crumb := zone.Mark(zoneBack, back) + sep + keyPart
	if m.title != "" {
		avail := m.width - lipgloss.Width(back) - lipgloss.Width(sep) - lipgloss.Width(keyPart) - 2 // "  " gap
		if avail > 1 {
			title := ansi.Truncate(components.Flatten(m.title), avail, "…")
			crumb += lipgloss.NewStyle().Foreground(t.Muted).Render("  " + title)
		}
	}
	return lipgloss.NewStyle().MaxWidth(m.width).Render(crumb)
}

// listPanel is a titled borderless panel matching Projects/Schema/Agents.
func (m Model) listPanel(h int) string {
	t := m.deps.Styles.Theme
	innerW := max(10, m.width-4)
	body := lipgloss.NewStyle().Width(innerW).Height(h - 2).MaxHeight(h - 2).Render(m.listBody(innerW, h-2))
	return zone.Mark("project.list", components.TitledRule(m.listTitle(), m.listCounter(), body, t.Primary))
}

func (m Model) listTitle() string {
	if m.loading {
		return "Issues (loading)"
	}
	return fmt.Sprintf("Issues (%d)", m.page.TotalElements)
}

func (m Model) listCounter() string {
	if m.loading || len(m.issues) == 0 {
		return ""
	}
	more := ""
	if m.page.HasNext {
		more = " · ↓ more"
	}
	return fmt.Sprintf("%d/%d%s", len(m.issues), m.page.TotalElements, more)
}

func (m Model) listBody(w, avail int) string {
	s := m.deps.Styles
	switch {
	case m.loading:
		return s.Muted.Render("Loading issues…")
	case m.loadErr:
		return s.Error.Render("Failed to load issues. Press r to retry.")
	case len(m.issues) == 0:
		return s.Muted.Render("No issues in this project yet.")
	}

	titleW := max(6, w-colAsg-colKey-colType-colState-colPri-5) // 5 single-column gaps
	rows := []string{m.headerRow(titleW)}

	// reserve the header row (and, when shown, the "Loading more…" row) out of the visible budget so
	// neither the indicator nor the last issue row is clipped by the panel's MaxHeight.
	chrome := 1
	if m.loadingMore {
		chrome++
	}
	visible := max(1, (avail-chrome)/rowH)
	top := listTop(m.cursor, visible, len(m.issues))
	for j := top; j < len(m.issues) && j < top+visible; j++ {
		rows = append(rows, zone.Mark(issueRowZone(j), m.issueRow(m.issues[j], j, titleW)))
	}
	if m.loadingMore {
		rows = append(rows, s.Muted.Render("Loading more…"))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (m Model) headerRow(titleW int) string {
	head := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Bold(true)
	cols := []string{
		pad(" ", colAsg), pad("Key", colKey), pad("Type", colType),
		pad("Title", titleW), pad("State", colState), pad("Pri", colPri),
	}
	return head.Render(strings.Join(cols, " "))
}

func (m Model) issueRow(it domain.IssueSummary, i, titleW int) string {
	t := m.deps.Styles.Theme
	sel := i == m.cursor
	base := lipgloss.NewStyle().Foreground(t.Text)
	if sel {
		base = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	}
	asg := lipgloss.NewStyle().Foreground(t.Muted).Render("○")
	if it.Assigned {
		asg = lipgloss.NewStyle().Foreground(t.Success).Render("●")
	}
	key := base.Render(pad(fit(it.Key, colKey), colKey))
	typ := lipgloss.NewStyle().Foreground(t.Muted).Render(pad(fit(it.TypeName, colType), colType))
	title := base.Render(pad(fit(it.Title, titleW), titleW))
	state := lipgloss.NewStyle().Foreground(stateColor(t, it.StateCategory)).Render(pad(fit(it.StateLabel, colState), colState))
	pri := lipgloss.NewStyle().Foreground(priorityColor(t, it.Priority)).Render(pad(it.Priority, colPri))
	return strings.Join([]string{asg, key, typ, title, state, pri}, " ")
}

func stateColor(t theme.Theme, cat string) color.Color {
	switch cat {
	case "COMPLETED":
		return t.Success
	case "ACTIVE":
		return t.Primary
	case "ABORTED":
		return t.Error
	default: // INITIAL
		return t.Muted
	}
}

func priorityColor(t theme.Theme, p string) color.Color {
	switch p {
	case "P0":
		return t.Error
	case "P1":
		return t.Warning
	default:
		return t.Muted
	}
}

func listTop(cursor, visible, n int) int {
	top := 0
	if cursor >= visible {
		top = cursor - visible + 1
	}
	if maxTop := n - visible; top > maxTop {
		top = maxTop
	}
	if top < 0 {
		top = 0
	}
	return top
}

// pad fits s to w cells. MaxHeight(1) stops an over-wide cell wrapping into a second row and breaking the table.
func pad(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).MaxWidth(w).MaxHeight(1).Render(s)
}

func fit(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).MaxWidth(w).MaxHeight(1).Render(components.Flatten(s))
}
