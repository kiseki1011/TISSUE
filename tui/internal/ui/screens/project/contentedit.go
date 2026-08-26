package project

import (
	"image/color"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textarea"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// the content editor's focusable controls, in tab order.
const (
	ceContent = iota
	cePreview
	ceSave
	ceCancel
)

// contentForm is the standalone "Edit content" modal, split out so the body has room and a preview.
type contentForm struct {
	deps    deps.Deps
	content textarea.Model
	preview bool // body shows rendered markdown instead of the textarea
	focus   int
	hover   int
}

func newContentForm(d deps.Deps, content string) contentForm {
	ta := textarea.New()
	ta.Prompt = ""
	ta.ShowLineNumbers = false
	ta.CharLimit = 0 // issue bodies can be long markdown
	ta.SetWidth(editFieldW)
	ta.SetHeight(editContentH)
	ta.SetValue(content)
	ta.Focus()
	return contentForm{deps: d, content: ta, focus: ceContent, hover: -1}
}

func (f contentForm) Init() tea.Cmd { return textarea.Blink }

// While previewing the body is read-only, so the textarea is not a tab stop.
func (f contentForm) fields() []int {
	if f.preview {
		return []int{cePreview, ceSave, ceCancel}
	}
	return []int{ceContent, cePreview, ceSave, ceCancel}
}

func (f contentForm) Update(msg tea.Msg) (contentForm, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		f.hover = f.hitZone(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	var cmd tea.Cmd
	f.content, cmd = f.content.Update(msg)
	return f, cmd
}

func (f contentForm) onKey(msg tea.KeyPressMsg) (contentForm, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return f, cancelContent
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != ceContent {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != ceContent {
			return f.moveFocus(1)
		}
	case "enter":
		switch f.focus {
		case cePreview:
			return f.togglePreview()
		case ceSave:
			return f.submit()
		case ceCancel:
			return f, cancelContent
		case ceContent:
			return f.typeIntoContent(msg) // a newline in the body
		}
	}
	return f.typeIntoContent(msg)
}

// Preview moves focus off the read-only body to the toggle. Leaving it refocuses the textarea.
func (f contentForm) togglePreview() (contentForm, tea.Cmd) {
	f.preview = !f.preview
	if f.preview {
		f.content.Blur()
		f.focus = cePreview
		return f, nil
	}
	f.focus = ceContent
	return f, f.content.Focus()
}

func (f contentForm) moveFocus(delta int) (contentForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	f.focus = fs[(cur+delta+len(fs))%len(fs)]
	if f.focus == ceContent {
		return f, f.content.Focus()
	}
	f.content.Blur()
	return f, nil
}

func (f contentForm) typeIntoContent(msg tea.KeyPressMsg) (contentForm, tea.Cmd) {
	if f.focus != ceContent {
		return f, nil
	}
	var cmd tea.Cmd
	f.content, cmd = f.content.Update(msg)
	return f, cmd
}

func (f contentForm) submit() (contentForm, tea.Cmd) {
	return f, submitContentEdit(f.content.Value())
}

func (f contentForm) onClick(msg tea.MouseClickMsg) (contentForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case ceContent:
		if f.preview {
			return f, nil // the preview body is read-only
		}
		f.focus = ceContent
		return f, f.content.Focus()
	case cePreview:
		return f.togglePreview()
	case ceSave:
		return f.submit()
	case ceCancel:
		return f, cancelContent
	}
	return f, nil
}

func (f contentForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range []int{ceContent, cePreview, ceSave, ceCancel} {
		if zone.Get(contentZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f contentForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("Edit content", body, f.deps.Styles.Theme.Primary)
}

func (f contentForm) body() string {
	var rows []string
	if f.preview {
		rows = append(rows, f.previewField())
	} else {
		rows = append(rows, f.field(ceContent, "Content", fixField(f.content.View(), editContentH), ""))
	}
	rows = append(rows, f.previewButton(), "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// previewField renders the body as markdown at the Content field's width. It is sanitized so a stray
// control char cannot corrupt the frame.
func (f contentForm) previewField() string {
	t := f.deps.Styles.Theme
	md := f.content.Value()
	var rendered string
	if strings.TrimSpace(md) == "" {
		rendered = f.deps.Styles.Muted.Render("Nothing to preview.")
	} else {
		rendered = components.Markdown(sanitizeBlock(md), editFieldW, components.IsDark(t.Background))
	}
	body := lipgloss.NewStyle().Width(editFieldW).Render(rendered)
	return components.TitledBoxWeighted("Preview", body, t.Primary, false)
}

// FocusRow reports the focused control's row/height (chromeTop = top border + the padding row).
func (f contentForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	bodyView := f.previewField()
	if !f.preview {
		bodyView = f.field(ceContent, "Content", fixField(f.content.View(), editContentH), "")
	}
	bodyH := lipgloss.Height(bodyView)
	toggle := f.previewButton()
	switch f.focus {
	case ceContent:
		return chromeTop, bodyH, true
	case cePreview:
		return chromeTop + bodyH, lipgloss.Height(toggle), true
	}
	// Save/Cancel sit after the body, the toggle, and the blank row before them
	return chromeTop + bodyH + lipgloss.Height(toggle) + 1, lipgloss.Height(f.buttons()), true
}

func (f contentForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	return zone.Mark(contentZone(id), box)
}

func (f contentForm) fieldBorderColor(id int, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.focus == id:
		return t.Accent
	case f.hover == id:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f contentForm) previewButton() string {
	label := "Preview"
	if f.preview {
		label = "Edit"
	}
	return f.button(label, cePreview)
}

func (f contentForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", ceSave),
		" ",
		f.button("Cancel", ceCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f contentForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(contentZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f contentForm) HelpKeys() []key.Binding {
	toggle := "preview"
	if f.preview {
		toggle = "edit"
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", toggle+"/save")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

func contentZone(id int) string {
	switch id {
	case ceContent:
		return "project.content.body"
	case cePreview:
		return "project.content.preview"
	case ceSave:
		return "project.content.save"
	case ceCancel:
		return "project.content.cancel"
	}
	return ""
}

type contentSubmittedMsg struct{ content string }

type contentCancelledMsg struct{}

func cancelContent() tea.Msg { return contentCancelledMsg{} }

func submitContentEdit(content string) tea.Cmd {
	return func() tea.Msg { return contentSubmittedMsg{content: content} }
}

// openContentEditor refuses while the detail is still loading, like the metadata edit form.
func (m Model) openContentEditor() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	m.editingContent = true
	m.contentScroll = 0
	m.contentBase = d.Content // diff the save against this snapshot, not a cache a refetch may change mid-edit
	m.contentUI = newContentForm(m.deps, d.Content)
	return m, m.contentUI.Init()
}

func (m Model) updateContentEditor(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case contentCancelledMsg:
		m.editingContent = false
		return m, nil
	case contentSubmittedMsg:
		return m.submitContent(msg.content)
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.contentUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.contentScroll = clampScroll(m.contentScroll-1, m.contentScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.contentScroll = clampScroll(m.contentScroll+1, m.contentScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.contentUI, cmd = m.contentUI.Update(msg)
	return m.followContentFocus(), cmd
}

func (m Model) contentScrollMax() int {
	return max(0, lipgloss.Height(m.contentUI.View())-m.height)
}

func (m Model) followContentFocus() Model {
	row, height, ok := m.contentUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.contentUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2
	off := m.contentScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.contentScroll = min(max(off, 0), boxH-m.height)
	return m
}

// submitContent reuses the edit flow's optimistic apply + reconcile via the common-fields PATCH.
func (m Model) submitContent(newContent string) (Model, tea.Cmd) {
	m.editingContent = false
	if _, ok := m.details[m.viewKey]; !ok {
		return m, nil
	}
	if newContent == m.contentBase {
		return m, toast.Show(toast.Info, "No changes.")
	}
	edit := domain.IssueEdit{Content: &newContent}
	m.applyEdit(m.viewKey, edit) // optimistic: the EditDoneMsg refetch reconciles
	return m, editIssue(m.deps, m.viewKey, edit)
}
