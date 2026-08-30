package inbox

import (
	"strconv"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
)

const (
	rowHeight = 1
	hInset    = 2 // left/right padding, so the feed lines up with the other tabs
	colGap    = 1 // the blank column between the list and detail panes

	stackBelowW = 90 // below this width, a tall-enough terminal stacks the panes
	stackMinH   = 20
)

func rowZone(i int) string { return "inbox.row." + strconv.Itoa(i) }

type layoutKind int

const (
	layoutSide layoutKind = iota
	layoutStacked
)

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	base := m.baseView()
	if m.prefsOpen {
		return m.overlayModal(base, m.prefsModalView())
	}
	return base
}

func (m Model) baseView() string {
	s := m.deps.Styles
	if m.width < minWidth || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Terminal too small"))
	}
	if m.loading && len(m.items) == 0 {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Loading notifications…"))
	}
	if m.loadErr {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Error.Render("Failed to load notifications."))
	}
	return m.dashboard()
}

func (m Model) dashboard() string {
	if m.layout() == layoutStacked {
		w, detailH := m.stackWidth(), m.stackDetailH()
		stack := lipgloss.JoinVertical(lipgloss.Left, m.detailPane(w, detailH), m.listPane(w, m.height-detailH))
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, stack)
	}
	leftW, rightW := m.panelWidths()
	dash := lipgloss.JoinHorizontal(lipgloss.Top, m.listPane(leftW, m.height), " ", m.detailPane(rightW, m.height))
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, dash)
}

func (m Model) layout() layoutKind {
	if components.StackVertically(m.width, m.height, stackBelowW, stackMinH) {
		return layoutStacked
	}
	return layoutSide
}

func (m Model) stackWidth() int   { return clamp(m.width-2*hInset, 24, 84) }
func (m Model) stackDetailH() int { return clamp(m.height*45/100, 8, m.height-6) }

// panelWidths splits the usable width 1:1, the detail pane taking the odd cell.
func (m Model) panelWidths() (int, int) {
	usable := m.width - 2*hInset - colGap
	left := usable / 2
	right := usable - left
	return left, right
}

func (m Model) listPane(w, h int) string {
	t := m.deps.Styles.Theme
	innerW := w - 4
	bodyH := max(1, h-2)
	rows := []string{""}
	headerH := 1 // the leading blank row

	label := "Inbox"
	switch {
	case m.unreadOnly && m.mentionsOnly:
		label = "Unread Mentions"
	case m.mentionsOnly:
		label = "Mentions"
	case m.unreadOnly:
		label = "Unread"
	}
	if len(m.items) == 0 {
		empty := "No notifications yet."
		switch {
		case m.unreadOnly && m.mentionsOnly:
			empty = "No unread mentions."
		case m.mentionsOnly:
			empty = "No mentions."
		case m.unreadOnly:
			empty = "No unread notifications."
		}
		rows = append(rows, m.deps.Styles.Muted.Render(empty))
	} else {
		visible := max(1, (bodyH-headerH)/rowHeight)
		top := listTop(m.cursor, visible, len(m.items))
		for j := top; j < len(m.items) && j < top+visible; j++ {
			rows = append(rows, zone.Mark(rowZone(j), m.notifRow(m.items[j], j, innerW, m.hover == rowZone(j))))
		}
	}
	body := lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	title := label + " (" + strconv.Itoa(len(m.items)) + ")"
	sub := ""
	if m.hasNext {
		sub = "scroll for more"
	}
	return zone.Mark("inbox.list", components.TitledRule(title, sub, body, t.Primary))
}

func (m Model) notifRow(n domain.Notification, i, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	nameStyle := lipgloss.NewStyle().Foreground(t.Text)
	switch {
	case i == m.cursor:
		nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	case n.IsRead:
		nameStyle = lipgloss.NewStyle().Foreground(t.Muted) // read items dim back
	}
	dot := "  "
	if !n.IsRead {
		dot = lipgloss.NewStyle().Foreground(t.Primary).Render("●") + " "
	}
	metaStyle := lipgloss.NewStyle().Foreground(t.Muted)
	meta := components.HumanizeSince(n.CreatedAt)
	if tg := n.Target(); tg != "" {
		meta = tg + "  " + meta
	}
	metaStr := metaStyle.Render(meta)
	headW := max(1, w-2-lipgloss.Width(metaStr)-1)
	line := dot + nameStyle.Render(fit(n.Headline(), headW)) + " " + metaStr
	if hovered && i != m.cursor {
		line = m.hoverBand().Width(w).Render(line)
	}
	return line
}

func (m Model) detailPane(w, h int) string {
	t := m.deps.Styles.Theme
	return zone.Mark("inbox.detail", components.TitledRule("Details", "", m.detailContent(w-4, max(1, h-2)), t.Primary))
}

func (m Model) detailContent(innerW, bodyH int) string {
	n, ok := m.selected()
	if !ok {
		return lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(
			m.deps.Styles.Muted.Render("Select a notification to see its details."))
	}
	t := m.deps.Styles.Theme
	g := m.deps.Glyphs
	key := lipgloss.NewStyle().Foreground(t.Muted)
	val := lipgloss.NewStyle().Foreground(t.Text)
	const labelW = 11
	row := func(glyph, k, v string) string {
		if glyph != "" {
			k = glyph + "  " + k
		}
		return key.Render(fit(k, labelW)) + val.Render(fit(v, max(1, innerW-labelW)))
	}

	head := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Width(innerW).Render(n.Headline())
	status := "Unread"
	if n.IsRead {
		status = "Read"
	}
	lines := []string{
		"", head, "",
		row(g.Or(g.Tag, ""), "Type", domain.HumanizeNotificationType(n.Type)),
		row(g.Or(g.Person, ""), "From", orDash(n.Actor())),
		row(g.Or(g.Calendar, ""), "When", fmtWhen(n)),
	}
	if p := detailProject(n); p != "" {
		lines = append(lines, row(g.Or(g.Project, ""), "Project", p))
	}
	if k := n.Ref.IssueKey; k != "" {
		lines = append(lines, row(g.Or(g.Identifier, ""), "Issue", k))
	}
	if title := n.Data["sprintTitle"]; title != "" {
		lines = append(lines, row(g.Or(g.Run, ""), "Sprint", title))
	}
	statusGlyph := g.Or(g.MailRead, "")
	if !n.IsRead {
		statusGlyph = g.Or(g.Mail, "")
	}
	lines = append(lines, row(statusGlyph, "Status", status), "")

	if body := n.Detail(); body != "" {
		lines = append(lines, m.rule(innerW), "", val.Width(innerW).Render(body))
	}
	block := lipgloss.JoinVertical(lipgloss.Left, lines...)
	return lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(block)
}

func detailProject(n domain.Notification) string {
	if n.Ref.ProjectKey != "" {
		return n.Ref.ProjectKey
	}
	return n.Data["projectKey"]
}

func fmtWhen(n domain.Notification) string {
	if n.CreatedAt.IsZero() {
		return "—"
	}
	return n.CreatedAt.Local().Format("2006-01-02 15:04") + " (" + components.HumanizeSince(n.CreatedAt) + " ago)"
}

// hoverBand bands a mouse-hovered row. The ANSI theme has no background to dim, so it tints text.
func (m Model) hoverBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	if _, noBg := t.Background.(lipgloss.NoColor); noBg {
		return lipgloss.NewStyle().Foreground(t.Secondary)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Background(components.MixColors(t.Selection, t.Background, 0.5))
}

func (m Model) rule(w int) string {
	if w < 0 {
		w = 0
	}
	return lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Border).Render(strings.Repeat("─", w))
}

// listTop is the first visible index that keeps cursor within a window of visible rows.
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

// fit clips s to exactly w cells. lipgloss Width() word-wraps an over-long string into extra rows,
// breaking the one-line-per-notification layout.
func fit(s string, w int) string {
	return components.FitLine(s, w)
}

func orDash(s string) string {
	if s == "" {
		return "—"
	}
	return s
}
