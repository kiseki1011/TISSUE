package project

import (
	"context"
	"strconv"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const activityPageSize = 30

// ActivitiesLoadedMsg is exported so the app shell can route it after the user leaves the drill-in.
type ActivitiesLoadedMsg struct {
	key  string
	gen  int
	page domain.IssueActivityPage
	err  bool
}

func loadActivities(d deps.Deps, key string, gen int) tea.Cmd {
	return func() tea.Msg {
		page, err := d.Issues.ListIssueActivities(context.Background(), key, activityPageSize)
		return ActivitiesLoadedMsg{key: key, gen: gen, page: page, err: err != nil}
	}
}

// startActivityLoad bumps the load generation so a superseded in-flight load is ignored when it lands.
func (m *Model) startActivityLoad(key string) tea.Cmd {
	m.activityGen[key]++
	m.activitiesPending[key] = true
	m.activitiesFailed[key] = false
	return loadActivities(m.deps, key, m.activityGen[key])
}

func (m *Model) maybeLoadActivity() tea.Cmd {
	if !m.showActivity || m.viewKey == "" {
		return nil
	}
	if _, cached := m.activities[m.viewKey]; cached {
		return nil
	}
	if m.activitiesPending[m.viewKey] {
		return nil
	}
	return m.startActivityLoad(m.viewKey)
}

// activityHelp names the view the v toggle switches to, not the one currently showing.
func (m Model) activityHelp() string {
	if m.showActivity {
		return "details"
	}
	return "activity"
}

func (m Model) activityBody(w int) string {
	s := m.deps.Styles
	if m.viewKey == "" {
		return s.Muted.Render("No issue selected.")
	}
	if p, ok := m.activities[m.viewKey]; ok {
		return m.activityContent(p, w)
	}
	if m.activitiesFailed[m.viewKey] {
		return s.Error.Render("Failed to load activity.") + "\n\n" + s.Muted.Render("Press R to retry.")
	}
	return activitySkeleton(s, w)
}

func (m Model) activityContent(p domain.IssueActivityPage, w int) string {
	s := m.deps.Styles
	if len(p.Items) == 0 {
		return s.Muted.Render("No activity.")
	}
	names := m.activityFieldNames()
	var rows []string
	for i, a := range p.Items {
		if i > 0 {
			rows = append(rows, "")
		}
		rows = append(rows, m.activityEntry(a, names, w)...)
	}
	if p.HasNext {
		rows = append(rows, "", s.Muted.Render("… earlier activity not shown"))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (m Model) activityEntry(a domain.IssueActivity, fieldNames map[string]string, w int) []string {
	t := m.deps.Styles.Theme
	rows := m.timelineLines(true, activityLabel(a.Type), lipgloss.NewStyle().Foreground(t.Text), w)
	if meta := m.activityMeta(a); meta != "" {
		rows = append(rows, m.timelineLines(false, meta, lipgloss.NewStyle().Foreground(t.Muted), w)...)
	}
	for _, line := range activityDetails(a, fieldNames) {
		rows = append(rows, m.timelineLines(false, line, lipgloss.NewStyle().Foreground(t.Text), w)...)
	}
	return rows
}

// activityFieldNames maps a custom field id to its label, for resolving customFields.{id} change keys.
func (m Model) activityFieldNames() map[string]string {
	d, ok := m.details[m.viewKey]
	if !ok {
		return nil
	}
	names := make(map[string]string, len(d.CustomFields))
	for _, f := range d.CustomFields {
		if f.ID != 0 && f.Label != "" {
			names[strconv.FormatInt(f.ID, 10)] = f.Label
		}
	}
	return names
}

// timelineLines renders one line onto the timeline gutter. Continuations keep the connector so the
// vertical line stays unbroken, and the untrusted server text is flattened first.
func (m Model) timelineLines(event bool, text string, content lipgloss.Style, w int) []string {
	t := m.deps.Styles.Theme
	bar := lipgloss.NewStyle().Foreground(t.Muted).Render("│") + " "
	lead := bar
	if event {
		lead = lipgloss.NewStyle().Foreground(t.Accent).Render("●") + " "
	}
	inner := max(1, w-2) // the gutter is two cells wide
	wrapped := lipgloss.NewStyle().Width(inner).Render(components.Flatten(text))
	lines := strings.Split(wrapped, "\n")
	out := make([]string, len(lines))
	for i, line := range lines {
		prefix := bar
		if i == 0 {
			prefix = lead
		}
		out[i] = prefix + content.Render(line)
	}
	return out
}

// activityMeta is the timeline's time-and-actor line. An unknown or system actor is omitted.
func (m Model) activityMeta(a domain.IssueActivity) string {
	parts := []string{formatRelative(a.OccurredAt)}
	if name := m.memberLabel(a.ActorID); name != "" {
		parts = append(parts, name)
	}
	return strings.Join(parts, " · ")
}

// memberLabel resolves an actor id to a member name, or "" for the system actor (0) or an unloaded one.
func (m Model) memberLabel(id int64) string {
	if id == 0 {
		return ""
	}
	for _, mem := range m.members {
		if mem.MemberID == id {
			return mem.Name()
		}
	}
	return ""
}

// formatRelative words today and yesterday in friendly form. Older times show an absolute date/time.
func formatRelative(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	// server instants are UTC: read them local first, else a Seoul morning reads as "yesterday at 23:30"
	t = t.Local()
	now := time.Now()
	hm := t.Format("15:04")
	ty, tm, td := t.Date()
	if ny, nm, nd := now.Date(); ty == ny && tm == nm && td == nd {
		return "today at " + hm
	}
	if yy, ym, yd := now.AddDate(0, 0, -1).Date(); ty == yy && tm == ym && td == yd {
		return "yesterday at " + hm
	}
	return t.Format("2006-01-02 15:04")
}

func activitySkeleton(s theme.Styles, w int) string {
	bar := func(frac float64) string {
		n := max(1, int(float64(w)*frac))
		return s.Muted.Render(strings.Repeat("░", n))
	}
	return lipgloss.JoinVertical(lipgloss.Left,
		s.Muted.Render("Loading activity…"), "",
		bar(0.5), bar(0.7), "",
		bar(0.5), bar(0.6),
	)
}

// activityLabel turns an event type into plain words, so a type this client never heard of still reads.
func activityLabel(eventType string) string {
	s := strings.ReplaceAll(strings.TrimPrefix(strings.TrimSpace(eventType), "ISSUE_"), "_", " ")
	if s == "" { // empty, or a bare "ISSUE_" that reduced to nothing
		return "Activity"
	}
	return strings.ToUpper(s[:1]) + strings.ToLower(s[1:])
}

// activityDataSkip lists data keys the entry already shows.
var activityDataSkip = map[string]bool{
	"issueKey": true, "actorName": true, "actorEmail": true, "projectKey": true,
	"oldState": true, "newState": true, "oldPoint": true, "newPoint": true,
	"oldParent": true, "newParent": true,
}

// activityDetails builds an event's detail lines. Content shows only "Content updated". A change with
// neither side is dropped, not shown as an empty diff.
func activityDetails(a domain.IssueActivity, fieldNames map[string]string) []string {
	var lines []string
	for _, ch := range a.Changes {
		label := changeLabel(ch.Field, fieldNames)
		if ch.Field == "content" {
			lines = append(lines, label+" updated")
			continue
		}
		switch {
		case ch.From != "" && ch.To != "":
			lines = append(lines, label+": "+ch.From+" → "+ch.To)
		case ch.To != "":
			lines = append(lines, label+": "+ch.To)
		case ch.From != "":
			lines = append(lines, label+": "+ch.From+" (cleared)")
		}
	}
	for _, kv := range a.Data {
		if activityDataSkip[kv.Key] || kv.Value == "" {
			continue
		}
		lines = append(lines, humanizeKey(kv.Key)+": "+kv.Value)
	}
	return lines
}

// changeLabel resolves a customFields.{id} key to its label, else a generic "Custom field {id}".
func changeLabel(field string, fieldNames map[string]string) string {
	const prefix = "customFields."
	if strings.HasPrefix(field, prefix) {
		id := field[len(prefix):]
		if name := fieldNames[id]; name != "" {
			return strings.ToUpper(name[:1]) + name[1:]
		}
		return "Custom field " + id
	}
	return humanizeKey(field)
}

// humanizeKey turns a field key into a label: "assigneeName" -> "Assignee", "storyPoint" -> "Story point".
// An already-capitalized key passes through.
func humanizeKey(key string) string {
	if key == "" {
		return key
	}
	if key[0] >= 'A' && key[0] <= 'Z' {
		return key
	}
	base := strings.TrimSuffix(key, "Name")
	base = strings.TrimSuffix(base, "Key")
	if base == "" {
		base = key
	}
	var b strings.Builder
	for i, r := range base {
		if i > 0 && r >= 'A' && r <= 'Z' {
			b.WriteByte(' ')
		}
		b.WriteRune(r)
	}
	s := b.String()
	return strings.ToUpper(s[:1]) + strings.ToLower(s[1:])
}
