package schema

import (
	"context"
	"errors"
	"net/http"
	"strconv"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

const (
	vfOpened = iota
	vfMerged
	vfSave
	vfCancel
)

const vcsRowW = 44

// vcsForm edits a workflow's VCS automation: the transition auto-fired when a linked PR is
// opened or merged, written back in one PATCH.
type vcsForm struct {
	deps        deps.Deps
	wfID        int
	transitions []domain.WorkflowTransition
	states      []domain.WorkflowState // to resolve a transition's source/target names for the picker

	openedID int // selected transition id for PR opened, 0 = none
	mergedID int // selected transition id for PR merged, 0 = none

	focus      int
	spinner    spinner.Model
	status     string
	submitting bool

	pickOpen  bool
	pick      picker
	pickField int // vfOpened or vfMerged
}

func newVcsForm(d deps.Deps, wfID int, transitions []domain.WorkflowTransition, states []domain.WorkflowState, openedID, mergedID int) vcsForm {
	return vcsForm{
		deps: d, wfID: wfID, transitions: transitions, states: states,
		openedID: openedID, mergedID: mergedID,
		spinner: spinner.New(), focus: vfOpened,
	}
}

func (f vcsForm) Init() tea.Cmd { return nil }

func (f vcsForm) Update(msg tea.Msg) (vcsForm, tea.Cmd) {
	switch msg := msg.(type) {
	case vcsFailedMsg:
		f.submitting = false
		f.status = msg.message
		return f, nil
	case spinner.TickMsg:
		var cmd tea.Cmd
		f.spinner, cmd = f.spinner.Update(msg)
		if !f.submitting {
			cmd = nil
		}
		return f, cmd
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f, nil
}

func (f vcsForm) onKey(msg tea.KeyPressMsg) (vcsForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	if f.pickOpen {
		return f.pickKey(msg), nil
	}
	switch msg.String() {
	case "up", "k":
		if f.focus > 0 {
			f.focus--
		}
	case "down", "j":
		if f.focus < vfCancel {
			f.focus++
		}
	case "tab":
		f.focus = (f.focus + 1) % (vfCancel + 1)
	case "shift+tab":
		f.focus = (f.focus - 1 + vfCancel + 1) % (vfCancel + 1)
	case "enter", "space":
		switch f.focus {
		case vfOpened, vfMerged:
			return f.openPicker(f.focus), nil
		case vfSave:
			return f.submit()
		case vfCancel:
			return f, cancelVcs
		}
	}
	return f, nil
}

func (f vcsForm) pickKey(msg tea.KeyPressMsg) vcsForm {
	switch msg.String() {
	case "up", "k":
		f.pick = f.pick.move(-1)
	case "down", "j":
		f.pick = f.pick.move(1)
	case "enter", "space":
		return f.applyPick()
	case "esc":
		f.pickOpen = false
	}
	return f
}

func (f vcsForm) fieldID(field int) int {
	if field == vfMerged {
		return f.mergedID
	}
	return f.openedID
}

// Each transition shows its source → target flow in parentheses so the direction is clear at a glance.
func (f vcsForm) openPicker(field int) vcsForm {
	opts := []pickerOption{{value: "0", label: "None"}}
	for _, tr := range f.transitions {
		label := tr.Label
		if flow := f.transitionFlow(tr); flow != "" {
			label += " (" + flow + ")"
		}
		opts = append(opts, pickerOption{value: itoa(tr.ID), label: label})
	}
	title := "PR opened → transition"
	if field == vfMerged {
		title = "PR merged → transition"
	}
	f.pick = newPicker(title, opts, itoa(f.fieldID(field)), 8, vcsRowW)
	f.pickField, f.pickOpen, f.status = field, true, ""
	return f
}

func (f vcsForm) applyPick() vcsForm {
	opt, ok := f.pick.selected()
	f.pickOpen = false
	if !ok {
		return f
	}
	id, _ := strconv.Atoi(opt.value)
	if f.pickField == vfMerged {
		f.mergedID = id
	} else {
		f.openedID = id
	}
	return f
}

func (f vcsForm) submit() (vcsForm, tea.Cmd) {
	f.submitting, f.status = true, ""
	return f, tea.Batch(saveVcs(f.deps, f.wfID, f.openedID, f.mergedID), f.spinner.Tick)
}

func (f vcsForm) transitionName(id int) string {
	if id != 0 {
		for _, tr := range f.transitions {
			if tr.ID == id {
				return tr.Label
			}
		}
	}
	return ""
}

func (f vcsForm) stateName(id int) string {
	for _, st := range f.states {
		if st.ID == id {
			return st.Label
		}
	}
	return ""
}

func (f vcsForm) transitionFlow(tr domain.WorkflowTransition) string {
	src, tgt := f.stateName(tr.SourceID), f.stateName(tr.TargetID)
	if src == "" || tgt == "" {
		return ""
	}
	return src + " → " + tgt
}

func (f vcsForm) View() string {
	if f.pickOpen {
		return f.pick.View(f.deps.Styles)
	}
	t := f.deps.Styles.Theme
	// the description is the widest fixed element, so it sets the modal's content width. The
	// field values and buttons right-align to it, landing flush at the modal's right edge
	desc := f.deps.Styles.Muted.Render("Run a transition automatically on a linked PR event.")
	w := lipgloss.Width(desc)
	rows := []string{
		desc,
		"",
		f.fieldRow(vfOpened, "PR opened", f.openedID, w),
		f.fieldRow(vfMerged, "PR merged", f.mergedID, w),
	}
	switch {
	case f.submitting:
		rows = append(rows, "", lipgloss.NewStyle().Foreground(t.Warning).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, "", f.deps.Styles.Error.Width(w).Render(f.status))
	}
	rows = append(rows, "", f.buttons(w), "", hintBar(f.deps.Styles, "enter", "change", "esc", "cancel"))
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("VCS Automation", body, t.Primary)
}

func (f vcsForm) fieldRow(field int, label string, id, w int) string {
	t := f.deps.Styles.Theme
	marker, labelStyle := "  ", lipgloss.NewStyle().Foreground(t.Text)
	if f.focus == field {
		marker = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ")
		labelStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	}
	value := f.deps.Styles.Muted.Render("None")
	if name := f.transitionName(id); name != "" {
		value = lipgloss.NewStyle().Foreground(t.Text).Render(name)
	}
	head := marker + labelStyle.Render(label)
	return zone.Mark(vcsFieldZone(field), alignRow(head, value, w, lipgloss.NewStyle()))
}

func (f vcsForm) buttons(w int) string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "vcs.save", f.focus == vfSave),
		" ",
		f.button("Cancel", "vcs.cancel", f.focus == vfCancel),
	)
	return lipgloss.PlaceHorizontal(w, lipgloss.Right, group)
}

func (f vcsForm) button(label, id string, focused bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	if focused {
		borderCol, textCol, bold = t.Accent, t.Accent, true
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBoxWeighted("", body, borderCol, focused))
}

func (f vcsForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.pickOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move"))}
	if f.focus == vfOpened || f.focus == vfMerged {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "change")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

func vcsFieldZone(field int) string {
	if field == vfMerged {
		return "vcs.merged"
	}
	return "vcs.opened"
}

func (f vcsForm) onClick(msg tea.MouseClickMsg) (vcsForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	if f.pickOpen {
		if i := f.pick.hitOption(msg); i >= 0 {
			f.pick.cursor = i
			return f.applyPick(), nil
		}
		return f, nil
	}
	switch {
	case zone.Get(vcsFieldZone(vfOpened)).InBounds(msg):
		f.focus = vfOpened
		return f.openPicker(vfOpened), nil
	case zone.Get(vcsFieldZone(vfMerged)).InBounds(msg):
		f.focus = vfMerged
		return f.openPicker(vfMerged), nil
	case zone.Get("vcs.save").InBounds(msg):
		f.focus = vfSave
		return f.submit()
	case zone.Get("vcs.cancel").InBounds(msg):
		return f, cancelVcs
	}
	return f, nil
}

type vcsSavedMsg struct{ wfID int }

type vcsFailedMsg struct{ message string }

type vcsCancelledMsg struct{}

func cancelVcs() tea.Msg { return vcsCancelledMsg{} }

func saveVcs(d deps.Deps, wfID, openedID, mergedID int) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.UpdateWorkflowVcsSettings(context.Background(), wfID, openedID, mergedID); err != nil {
			return vcsFailedMsg{message: vcsErrorMessage(err)}
		}
		return vcsSavedMsg{wfID: wfID}
	}
}

func vcsErrorMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusBadRequest:
			return "Invalid VCS configuration."
		case http.StatusForbidden:
			return "You do not have permission to edit this."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // the server explained the failure; prefer it over the generic line
	}
	return "Could not save VCS settings. Try again."
}
