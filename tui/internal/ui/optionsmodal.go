package ui

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"strconv"
	"strings"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	sectionInfo = iota
	sectionSettings
	sectionAccount
	sectionCount
)

const (
	pickerNone = iota
	pickerTheme
	pickerIcons
	pickerPosition
)

const optionsWidth = 50

var sectionLabels = []string{"Info", "Settings", "Account"}

// themeSelectedMsg asks the shell to switch the whole app to the named theme (and persist it).
type themeSelectedMsg struct{ name string }

// iconsSelectedMsg asks the shell to switch the glyph set (and persist it).
type iconsSelectedMsg struct{ mode string }

// mouseSelectedMsg asks the shell to toggle mouse capture (and persist it).
type mouseSelectedMsg struct{ on bool }

// logoutMsg asks the shell to drop the session and return to login.
type logoutMsg struct{}

func logout() tea.Msg { return logoutMsg{} }

type optionsPositionsLoaded struct {
	positions []domain.PositionSummary
	err       error
}

// optionsPositionSet reports a successful save. It carries the id/name that were actually sent so the
// handler applies THAT (not a value the user may have re-picked while the request was in flight).
// positionID is 0 when cleared.
type optionsPositionSet struct {
	positionID int
	name       string
	cleared    bool
}

type optionsPositionFailed struct{ message string }

func loadOptionPositions(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		ps, err := d.Catalog.ListPositions(context.Background())
		return optionsPositionsLoaded{positions: ps, err: err}
	}
}

func setOptionPosition(d deps.Deps, id *int, name string, cleared bool) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.SetMyPosition(context.Background(), id); err != nil {
			return optionsPositionFailed{message: positionErrMessage(err)}
		}
		sent := 0
		if id != nil {
			sent = *id
		}
		return optionsPositionSet{positionID: sent, name: name, cleared: cleared}
	}
}

func positionErrMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) && apiErr.Status == http.StatusForbidden {
		return "Not allowed."
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // the server explained the failure; prefer it over the generic line
	}
	return "Could not update. Try again."
}

// optionsModal is the settings overlay with three sections: Info (server), Settings (theme), and
// Account (the caller's own info, an editable self-service position, and logout). The modal is mostly
// presentational (theme/logout effects are the shell's, driven by the messages it emits) but repaints
// itself in the new theme immediately and owns the position picker's async state.
type optionsModal struct {
	deps  deps.Deps
	theme theme.Theme // live-preview theme (may lead deps after a switch)
	user  domain.Profile

	themes  []string
	themeIx int // index of the applied theme

	iconsModes []string
	iconsIx    int

	mouseOn bool

	section int
	focus   int // control index within the current section

	positions        []domain.PositionSummary
	positionsLoading bool
	positionsErr     error
	positionSaving   bool
	positionStatus   string // last save error, shown inline under Position

	picking int // pickerNone | pickerTheme | pickerPosition
	pick    widgets.ListPicker
}

func newOptionsModal(d deps.Deps, user domain.Profile) (optionsModal, tea.Cmd) {
	themes := theme.Names()
	ix := 0
	for i, n := range themes {
		if n == d.Styles.Theme.Name {
			ix = i
		}
	}
	iconsModes := []string{"auto", "nerd", "unicode"}
	iconsIx := 0
	for i, n := range iconsModes {
		if n == d.Icons {
			iconsIx = i
		}
	}
	m := optionsModal{
		deps: d, theme: d.Styles.Theme, user: user,
		themes: themes, themeIx: ix,
		iconsModes: iconsModes, iconsIx: iconsIx,
		mouseOn: d.Mouse,
		section: sectionInfo, positionsLoading: true,
	}
	return m, loadOptionPositions(d)
}

func (m optionsModal) controls() int {
	switch m.section {
	case sectionSettings:
		return 3 // theme, icons, mouse
	case sectionAccount:
		return 2 // position, logout
	default:
		return 0 // Info is read-only
	}
}

func (m optionsModal) onThemeControl() bool { return m.section == sectionSettings && m.focus == 0 }
func (m optionsModal) onIconsControl() bool { return m.section == sectionSettings && m.focus == 1 }
func (m optionsModal) onMouseControl() bool { return m.section == sectionSettings && m.focus == 2 }
func (m optionsModal) onPositionControl() bool {
	return m.section == sectionAccount && m.focus == 0
}
func (m optionsModal) onLogoutControl() bool { return m.section == sectionAccount && m.focus == 1 }

func (m optionsModal) Update(msg tea.Msg) (appModal, tea.Cmd) {
	switch msg := msg.(type) {
	case optionsPositionsLoaded:
		m.positionsLoading = false
		m.positionsErr = msg.err
		m.positions = msg.positions
		return m, nil
	case optionsPositionSet:
		m.positionSaving = false
		m.positionStatus = ""
		// reflect exactly what was saved (from the message), not a value the user may have re-picked
		// while the request was in flight
		m.user.PositionID, m.user.PositionName = msg.positionID, msg.name
		return m, toast.Show(toast.Success, positionToastText(msg.name, msg.cleared))
	case optionsPositionFailed:
		m.positionSaving = false
		m.positionStatus = msg.message
		return m, toast.Show(toast.Error, msg.message)
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	}
	return m, nil
}

func positionToastText(name string, cleared bool) string {
	if cleared {
		return "Cleared your position."
	}
	return "Set your position to \"" + name + "\"."
}

func (m optionsModal) onKey(msg tea.KeyPressMsg) (appModal, tea.Cmd) {
	if m.picking != pickerNone {
		return m.pickerKey(msg)
	}
	switch msg.String() {
	case "esc":
		return m, closeModal
	case "tab", "right", "l":
		return m.switchSection(1)
	case "shift+tab", "left", "h":
		return m.switchSection(-1)
	case "up", "k":
		if m.focus > 0 {
			m.focus--
		}
		return m, nil
	case "down", "j":
		if m.focus < m.controls()-1 {
			m.focus++
		}
		return m, nil
	case "enter", "space":
		return m.activate()
	}
	return m, nil
}

// switchSection moves to the neighbouring section, resetting focus and dropping a stale picker error
// so it cannot follow the user across sections.
func (m optionsModal) switchSection(delta int) (appModal, tea.Cmd) {
	m.section = (m.section + delta + sectionCount) % sectionCount
	m.focus = 0
	m.positionStatus = ""
	return m, nil
}

func (m optionsModal) activate() (appModal, tea.Cmd) {
	switch {
	case m.onThemeControl():
		return m.openThemePicker(), nil
	case m.onIconsControl():
		return m.openIconsPicker(), nil
	case m.onMouseControl():
		return m.toggleMouse()
	case m.onPositionControl():
		return m.openPositionPicker(), nil
	case m.onLogoutControl():
		return m, logout
	}
	return m, nil
}

func (m optionsModal) pickerKey(msg tea.KeyPressMsg) (appModal, tea.Cmd) {
	switch msg.String() {
	case "esc":
		m.picking = pickerNone
		return m, nil
	case "up", "k":
		m.pick = m.pick.Move(-1)
		return m, nil
	case "down", "j":
		m.pick = m.pick.Move(1)
		return m, nil
	case "enter", "space":
		return m.applyPick()
	}
	return m, nil
}

func (m optionsModal) openThemePicker() optionsModal {
	opts := make([]widgets.PickerOption, len(m.themes))
	for i, n := range m.themes {
		opts[i] = widgets.PickerOption{Value: n, Label: themeLabel(n)}
	}
	m.pick = widgets.NewListPicker("Theme", opts, m.themes[m.themeIx], len(m.themes), 22)
	m.picking = pickerTheme
	return m
}

func (m optionsModal) openIconsPicker() optionsModal {
	opts := make([]widgets.PickerOption, len(m.iconsModes))
	for i, n := range m.iconsModes {
		opts[i] = widgets.PickerOption{Value: n, Label: iconsLabel(n)}
	}
	m.pick = widgets.NewListPicker("Icons", opts, m.iconsModes[m.iconsIx], len(m.iconsModes), 22)
	m.picking = pickerIcons
	return m
}

func (m optionsModal) selectIcons(mode string) (appModal, tea.Cmd) {
	for i, n := range m.iconsModes {
		if n == mode {
			m.iconsIx = i
		}
	}
	return m, func() tea.Msg { return iconsSelectedMsg{mode: mode} }
}

// toggleMouse flips mouse capture. Unlike the pickers it acts in place (no drill-in), repainting the
// checkbox immediately and asking the shell to apply and persist the new state.
func (m optionsModal) toggleMouse() (appModal, tea.Cmd) {
	m.mouseOn = !m.mouseOn
	on := m.mouseOn
	return m, func() tea.Msg { return mouseSelectedMsg{on: on} }
}

// openPositionPicker stays closed while the list is loading or failed to load — there is nothing to
// choose.
func (m optionsModal) openPositionPicker() optionsModal {
	if !m.positionEditable() {
		return m
	}
	opts := make([]widgets.PickerOption, 0, len(m.positions)+1)
	opts = append(opts, widgets.PickerOption{Value: "", Label: "— (none)"})
	for _, p := range m.positions {
		opts = append(opts, widgets.PickerOption{Value: strconv.Itoa(p.ID), Label: p.Name})
	}
	m.pick = widgets.NewListPicker("Position", opts, positionValue(m.user.PositionID), min(1+len(m.positions), 8), 24)
	m.picking = pickerPosition
	return m
}

func (m optionsModal) applyPick() (appModal, tea.Cmd) {
	which := m.picking
	sel, ok := m.pick.Selected()
	m.picking = pickerNone
	if !ok {
		return m, nil
	}
	switch which {
	case pickerTheme:
		return m.selectTheme(sel.Value)
	case pickerIcons:
		return m.selectIcons(sel.Value)
	case pickerPosition:
		return m.selectPosition(sel.Value)
	}
	return m, nil
}

func (m optionsModal) selectTheme(name string) (appModal, tea.Cmd) {
	for i, n := range m.themes {
		if n == name {
			m.themeIx = i
		}
	}
	m.theme = theme.ByName(name)
	return m, func() tea.Msg { return themeSelectedMsg{name: name} }
}

func (m optionsModal) selectPosition(value string) (appModal, tea.Cmd) {
	m.positionStatus = ""
	if value == positionValue(m.user.PositionID) {
		return m, nil // nothing changed
	}
	id, name, cleared, ok := positionSetArgs(value, m.positions)
	if !ok {
		return m, nil
	}
	m.positionSaving = true
	return m, setOptionPosition(m.deps, id, name, cleared)
}

// positionSetArgs resolves a picker value into the arguments for setOptionPosition: value "" clears
// (nil id, cleared=true). A numeric value selects that position (its id + resolved name). ok is false
// only for a non-empty value that does not parse, which the caller treats as a no-op.
func positionSetArgs(value string, positions []domain.PositionSummary) (id *int, name string, cleared, ok bool) {
	if value == "" {
		return nil, "", true, true
	}
	n, err := strconv.Atoi(value)
	if err != nil {
		return nil, "", false, false
	}
	return &n, positionName(positions, n), false, true
}

func (m optionsModal) onClick(msg tea.MouseClickMsg) (appModal, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if m.picking != pickerNone {
		if i := m.pick.HitOption(msg); i >= 0 {
			m.pick.Cursor = i
			return m.applyPick()
		}
		return m, nil
	}
	for i := range sectionLabels {
		if zone.Get(sectionZone(i)).InBounds(msg) {
			m.section, m.focus = i, 0
			m.positionStatus = ""
			return m, nil
		}
	}
	switch {
	case zone.Get("opt.theme").InBounds(msg):
		m.section, m.focus = sectionSettings, 0
		return m.openThemePicker(), nil
	case zone.Get("opt.icons").InBounds(msg):
		m.section, m.focus = sectionSettings, 1
		return m.openIconsPicker(), nil
	case zone.Get("opt.mouse").InBounds(msg):
		m.section, m.focus = sectionSettings, 2
		return m.toggleMouse()
	case zone.Get("opt.pos").InBounds(msg):
		m.section, m.focus = sectionAccount, 0
		return m.openPositionPicker(), nil
	case zone.Get("opt.logout").InBounds(msg):
		m.section, m.focus = sectionAccount, 1
		return m, logout
	}
	return m, nil
}

func (m optionsModal) View() string {
	if m.picking != pickerNone {
		// drill in: the popup replaces the options body and the shell centers it, matching schema's
		// color/list pickers. It renders in the modal's live theme, not the (stale) open-time deps.
		box := m.pick.View(theme.New(m.theme))
		if m.picking == pickerTheme {
			return lipgloss.JoinHorizontal(lipgloss.Top, box, " ", m.themeSwatches(m.pickedTheme()))
		}
		return box
	}
	body := lipgloss.NewStyle().Padding(1).Render(lipgloss.JoinVertical(lipgloss.Left,
		m.segmentBar(),
		lipgloss.NewStyle().Foreground(m.theme.Muted).Render(strings.Repeat("─", optionsWidth)),
		m.sectionBody(),
	))
	return components.TitledBoxCentered("Options", body, m.theme.Accent)
}

func (m optionsModal) segmentBar() string {
	t := m.theme
	parts := make([]string, len(sectionLabels))
	for i, label := range sectionLabels {
		st := lipgloss.NewStyle().Foreground(t.Muted)
		if i == m.section {
			st = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
		parts[i] = zone.Mark(sectionZone(i), st.Render(label))
	}
	sep := lipgloss.NewStyle().Foreground(t.Muted).Render("   ")
	return strings.Join(parts, sep)
}

func (m optionsModal) sectionBody() string {
	switch m.section {
	case sectionSettings:
		return m.settingsBody()
	case sectionAccount:
		return m.accountBody()
	default:
		return m.infoBody()
	}
}

func (m optionsModal) head(s string) string {
	return lipgloss.NewStyle().Foreground(m.theme.Text).Bold(true).Render(s)
}

func (m optionsModal) infoBody() string {
	rows := []string{
		m.head("Server"),
		indentValue(m.theme, m.serverValue()),
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// settingsLabelW is the Muted label column shared by every Settings row, wide enough for the longest
// label ("Interactive Mouse") so the controls line up in a single column.
const settingsLabelW = 19

func (m optionsModal) settingsBody() string {
	rows := []string{
		m.settingRow("Theme", m.themeButton()),
		m.settingRow("Icons", m.iconsButton()),
		m.settingRow("Interactive Mouse", m.mouseButton()),
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// settingRow lays a control on the same line as its Muted label, matching the Account section's rows.
// The label stays Muted always; the control carries the focus accent.
func (m optionsModal) settingRow(label, control string) string {
	l := lipgloss.NewStyle().Foreground(m.theme.Muted).Width(settingsLabelW).Render(label)
	return "  " + l + control
}

func (m optionsModal) accountBody() string {
	t := m.theme
	rows := []string{
		m.head("Profile"),
		infoRow(t, "Name", m.user.Name),
		infoRow(t, "Username", m.user.Username),
		infoRow(t, "Email", m.user.Email),
		infoRow(t, "Role", roleLabel(m.user.Role)),
		infoRow(t, "Team", orDash(m.user.TeamName)),
		m.positionRow(),
	}
	if status := m.positionStatusLine(); status != "" {
		rows = append(rows, status)
	}
	rows = append(rows, "", rightAlignBlock(m.logoutButton(), optionsWidth))
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (m optionsModal) serverValue() string {
	if m.deps.Server == "" {
		return "-"
	}
	return m.deps.Server
}

func (m optionsModal) themeButton() string {
	return zone.Mark("opt.theme", m.dropdownButton(themeLabel(m.themes[m.themeIx]), m.onThemeControl()))
}

func (m optionsModal) iconsButton() string {
	return zone.Mark("opt.icons", m.dropdownButton(iconsLabel(m.iconsModes[m.iconsIx]), m.onIconsControl()))
}

// mouseButton renders mouse capture as a checkbox toggle (enter/click flips it) rather than a
// drop-in picker, since it is a two-state setting. The row label already names it, so the box carries
// no "Enabled" caption. The check glyph is BMP and our runewidth is EastAsianWidth=false, so it stays
// width 1 and does not shift the line.
func (m optionsModal) mouseButton() string {
	t := m.theme
	box := "[ ]"
	if m.mouseOn {
		box = "[✓]"
	}
	col := t.Text
	if m.onMouseControl() {
		col = t.Accent
	}
	label := lipgloss.NewStyle().Foreground(col).Bold(m.onMouseControl()).Render(box)
	return zone.Mark("opt.mouse", label)
}

// positionEditable gates the dropdown affordance, the enter/click action, and the footer hint together
// so they never disagree.
func (m optionsModal) positionEditable() bool {
	return !m.positionsLoading && m.positionsErr == nil && !m.positionSaving
}

// positionRow shows the position as the same "value ▾" dropdown control the Theme setting uses, so
// both self-service selectors read alike. While the list is loading/unavailable, or a save is in
// flight, the dropdown is withheld (a plain value, nothing to pick yet).
func (m optionsModal) positionRow() string {
	t := m.theme
	label := lipgloss.NewStyle().Foreground(t.Muted).Width(10).Render("Position")
	if !m.positionEditable() {
		var value string
		switch {
		case m.positionsLoading:
			value = "Loading…"
		case m.positionsErr != nil:
			value = "Unavailable."
		default:
			value = m.currentPositionLabel() // a save is in flight — show the (still-applied) value
		}
		return "  " + label + lipgloss.NewStyle().Foreground(t.Text).Render(value)
	}
	// the dropdown carries the click zone that opens the picker (registered only while editable, so a
	// stale click cannot fire)
	control := zone.Mark("opt.pos", m.dropdownButton(m.currentPositionLabel(), m.onPositionControl()))
	return "  " + label + control
}

func (m optionsModal) positionStatusLine() string {
	t := m.theme
	const indent = "            " // 2 (row inset) + 10 (label width)
	switch {
	case m.positionSaving:
		return lipgloss.NewStyle().Foreground(t.Warning).Render(indent + "saving…")
	case m.positionStatus != "":
		return lipgloss.NewStyle().Foreground(t.Error).Render(indent + m.positionStatus)
	}
	return ""
}

func (m optionsModal) pickedTheme() theme.Theme {
	if sel, ok := m.pick.Selected(); ok {
		return theme.ByName(sel.Value)
	}
	return m.theme
}

// themeSwatches renders a theme's palette as labelled colour boxes. The boxes and border use the
// previewed theme's colours. The labels stay in the live theme so they read on the backdrop.
func (m optionsModal) themeSwatches(preview theme.Theme) string {
	type swatch struct {
		label string
		c     color.Color
	}
	palette := []swatch{
		{"Accent", preview.Accent}, {"Primary", preview.Primary}, {"Secondary", preview.Secondary},
		{"Success", preview.Success}, {"Warning", preview.Warning}, {"Error", preview.Error},
		{"Text", preview.Text}, {"Muted", preview.Muted},
	}
	rows := make([]string, len(palette))
	for i, s := range palette {
		// a NoColor role (like the ANSI theme's Text = terminal default) has no paintable swatch.
		// Show a neutral "no fill" marker instead of a blank gap that reads as broken
		box := lipgloss.NewStyle().Background(s.c).Render("   ")
		if _, none := s.c.(lipgloss.NoColor); none {
			box = lipgloss.NewStyle().Foreground(m.theme.Muted).Render(" - ")
		}
		label := lipgloss.NewStyle().Foreground(m.theme.Muted).Render(s.label)
		rows[i] = box + " " + label
	}
	body := lipgloss.NewStyle().Padding(0, 1).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(themeLabel(preview.Name), body, preview.Primary)
}

func (m optionsModal) dropdownButton(label string, focused bool) string {
	t := m.theme
	nameStyle := lipgloss.NewStyle().Foreground(t.Text)
	arrow := lipgloss.NewStyle().Foreground(t.Muted)
	if focused {
		nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		arrow = lipgloss.NewStyle().Foreground(t.Accent)
	}
	name := lipgloss.PlaceHorizontal(16, lipgloss.Left, nameStyle.Render(label))
	return name + " " + arrow.Render("▾")
}

func (m optionsModal) currentPositionLabel() string {
	if m.user.PositionID == 0 {
		return "— (none)"
	}
	if m.user.PositionName != "" {
		return m.user.PositionName
	}
	if name := positionName(m.positions, m.user.PositionID); name != "" {
		return name
	}
	return "—"
}

func (m optionsModal) logoutButton() string {
	t := m.theme
	border, textCol := t.Error, t.Text
	if m.onLogoutControl() {
		border, textCol = t.Accent, t.Accent
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(m.onLogoutControl()).Render("Log out")
	return zone.Mark("opt.logout", components.TitledBox("", body, border))
}

func (m optionsModal) HelpKeys() []key.Binding {
	if m.picking != pickerNone {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab", "left", "right"), key.WithHelp("←/→", "section"))}
	if m.controls() > 1 {
		binds = append(binds, key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "field")))
	}
	switch {
	case m.onThemeControl():
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "theme")))
	case m.onIconsControl():
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "icons")))
	case m.onMouseControl():
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "toggle")))
	case m.onPositionControl():
		// only advertise the action when the position can actually be edited (list loaded, not
		// saving). Otherwise enter is a no-op and the hint would promise nothing
		if m.positionEditable() {
			binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "edit")))
		}
	case m.onLogoutControl():
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "log out")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "close")))
}

func sectionZone(i int) string { return "opt.section." + sectionLabels[i] }

func positionValue(id int) string {
	if id == 0 {
		return ""
	}
	return strconv.Itoa(id)
}

func positionName(positions []domain.PositionSummary, id int) string {
	for _, p := range positions {
		if p.ID == id {
			return p.Name
		}
	}
	return ""
}

func orDash(s string) string {
	if s == "" {
		return "—"
	}
	return s
}

// rightAlignBlock left-pads a possibly multi-line block so it sits flush right within w. It pads each
// line by hand rather than using lipgloss.PlaceHorizontal, which mis-measures a block carrying
// bubblezone markers (its width probe trips on the marker) and shreds the alignment.
func rightAlignBlock(block string, w int) string {
	pad := w - lipgloss.Width(block)
	if pad <= 0 {
		return block
	}
	prefix := strings.Repeat(" ", pad)
	lines := strings.Split(block, "\n")
	for i := range lines {
		lines[i] = prefix + lines[i]
	}
	return strings.Join(lines, "\n")
}

func infoRow(t theme.Theme, label, value string) string {
	if value == "" {
		value = "-"
	}
	l := lipgloss.NewStyle().Foreground(t.Muted).Width(10).Render(label)
	return "  " + l + lipgloss.NewStyle().Foreground(t.Text).Render(value)
}

func indentValue(t theme.Theme, value string) string {
	return "  " + lipgloss.NewStyle().Foreground(t.Text).Render(value)
}

func themeLabel(name string) string {
	parts := strings.Split(name, "-")
	for i, p := range parts {
		if p == "ansi" {
			parts[i] = "ANSI"
			continue
		}
		if p != "" {
			parts[i] = strings.ToUpper(p[:1]) + p[1:]
		}
	}
	return strings.Join(parts, " ")
}

func iconsLabel(mode string) string {
	switch mode {
	case "nerd":
		return "Nerd Font"
	case "unicode":
		return "Unicode"
	default:
		return "Auto"
	}
}

func roleLabel(role string) string {
	switch role {
	case "SUPER_ADMIN":
		return "Super Admin"
	case "ADMIN":
		return "Admin"
	case "USER":
		return "User"
	case "":
		return "-"
	default:
		return role
	}
}
