package project

import (
	"context"
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
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// the review modal's focusable controls, in tab order. Which exist depends on the caller's role: verdict
// for a reviewer, re-request only once someone has responded.
const (
	rvApprove = iota
	rvReject
	rvFeedback
	rvSubmit
	rvRerequest
	rvCancel
)

const reviewFeedbackH = 5

// reviewForm serves both sides of the review conversation: a reviewer submits a verdict, and anyone can
// ask reviewers who already responded to look again. One modal, since splitting costs a second key.
type reviewForm struct {
	deps         deps.Deps
	canReview    bool // the caller is a reviewer, so a verdict may be submitted
	rerequestIDs []int64
	responded    []string // display names, for the re-request line
	approved     bool     // the selected verdict. false means "request changes"
	feedback     textarea.Model
	focus        int
	hover        int
	sending      bool
	status       string // a failure line kept on the modal so the reason survives the toast
}

func newReviewForm(d deps.Deps, canReview bool, rerequestIDs []int64, responded []string) reviewForm {
	ta := textarea.New()
	ta.Prompt = ""
	ta.ShowLineNumbers = false
	ta.CharLimit = 10000 // the server's cap on a comment body, which this becomes
	ta.SetWidth(editFieldW)
	ta.SetHeight(reviewFeedbackH)
	ta.Placeholder = "Optional"

	f := reviewForm{
		deps:         d,
		canReview:    canReview,
		rerequestIDs: rerequestIDs,
		responded:    responded,
		approved:     true,
		feedback:     ta,
		hover:        -1,
	}
	f.focus = f.fields()[0]
	return f
}

func (f reviewForm) Init() tea.Cmd { return textarea.Blink }

// fields is the tab ring, built from what the caller may actually do. Cancel is always last.
func (f reviewForm) fields() []int {
	var fs []int
	if f.canReview {
		fs = append(fs, rvApprove, rvReject, rvFeedback, rvSubmit)
	}
	if len(f.rerequestIDs) > 0 {
		fs = append(fs, rvRerequest)
	}
	return append(fs, rvCancel)
}

func (f reviewForm) Update(msg tea.Msg) (reviewForm, tea.Cmd) {
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
	f.feedback, cmd = f.feedback.Update(msg)
	return f, cmd
}

func (f reviewForm) onKey(msg tea.KeyPressMsg) (reviewForm, tea.Cmd) {
	if f.sending {
		return f, nil // a submit is in flight, so ignore input until it lands
	}
	switch msg.String() {
	case "esc":
		return f, cancelReview
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != rvFeedback {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != rvFeedback {
			return f.moveFocus(1)
		}
	case "left", "right":
		// the verdict buttons are one two-sided segmented control, so either horizontal key flips it
		if f.focus == rvApprove || f.focus == rvReject {
			return f.pickVerdict(f.focus == rvReject)
		}
	case "space":
		// space commits the focused button, which tab may have moved off the selected one
		if f.focus == rvApprove || f.focus == rvReject {
			return f.pickVerdict(f.focus == rvApprove)
		}
	case "enter":
		switch f.focus {
		case rvApprove:
			return f.pickVerdict(true)
		case rvReject:
			return f.pickVerdict(false)
		case rvSubmit:
			return f.submit()
		case rvRerequest:
			return f, requestReReview
		case rvCancel:
			return f, cancelReview
		}
	}
	return f.typeIntoFeedback(msg)
}

// pickVerdict moves focus onto the chosen button, so selection and cursor never disagree.
func (f reviewForm) pickVerdict(approved bool) (reviewForm, tea.Cmd) {
	f.approved = approved
	f.focus = rvReject
	if approved {
		f.focus = rvApprove
	}
	f.feedback.Blur()
	return f, nil
}

func (f reviewForm) moveFocus(delta int) (reviewForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	f.focus = fs[(cur+delta+len(fs))%len(fs)]
	if f.focus == rvFeedback {
		return f, f.feedback.Focus()
	}
	f.feedback.Blur()
	return f, nil
}

func (f reviewForm) typeIntoFeedback(msg tea.KeyPressMsg) (reviewForm, tea.Cmd) {
	if f.focus != rvFeedback {
		return f, nil
	}
	var cmd tea.Cmd
	f.feedback, cmd = f.feedback.Update(msg)
	return f, cmd
}

func (f reviewForm) submit() (reviewForm, tea.Cmd) {
	f.sending = true
	f.status = ""
	f.feedback.Blur()
	return f, submitReview(f.approved, strings.TrimSpace(f.feedback.Value()))
}

func (f reviewForm) onClick(msg tea.MouseClickMsg) (reviewForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.sending {
		return f, nil
	}
	switch f.hitZone(msg) {
	case rvApprove:
		return f.pickVerdict(true)
	case rvReject:
		return f.pickVerdict(false)
	case rvFeedback:
		f.focus = rvFeedback
		return f, f.feedback.Focus()
	case rvSubmit:
		return f.submit()
	case rvRerequest:
		return f, requestReReview
	case rvCancel:
		return f, cancelReview
	}
	return f, nil
}

func (f reviewForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(reviewZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f reviewForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("Review", body, f.deps.Styles.Theme.Primary)
}

func (f reviewForm) body() string {
	s := f.deps.Styles
	var rows []string
	if f.canReview {
		rows = append(rows,
			s.Muted.Render("Your verdict"),
			f.verdictBar(),
			"",
			f.field(rvFeedback, "Feedback", fixField(f.feedback.View(), reviewFeedbackH)),
		)
	} else {
		rows = append(rows, lipgloss.NewStyle().Width(editFieldW).Foreground(s.Theme.Muted).
			Render("You are not a reviewer on this issue, so you cannot submit a verdict."))
	}
	if len(f.rerequestIDs) > 0 {
		rows = append(rows, "", s.Muted.Render(f.rerequestLine()))
	}
	if f.status != "" {
		rows = append(rows, "", lipgloss.NewStyle().Width(editFieldW).
			Foreground(s.Theme.Error).Render(f.status))
	}
	return lipgloss.JoinVertical(lipgloss.Left, append(rows, "", f.buttons())...)
}

// rerequestLine names who would be asked again, so the button is not a blind broadcast.
func (f reviewForm) rerequestLine() string {
	if len(f.responded) == 1 {
		return "Already reviewed: " + f.responded[0]
	}
	return "Already reviewed: " + strings.Join(f.responded, ", ")
}

// verdictBar's selected side carries the status colour it will write, matching how the verdict reads back.
func (f reviewForm) verdictBar() string {
	t := f.deps.Styles.Theme
	return lipgloss.JoinHorizontal(lipgloss.Top,
		f.verdictButton("Approve", rvApprove, f.approved, t.Success),
		" ",
		f.verdictButton("Request changes", rvReject, !f.approved, t.Error),
	)
}

func (f reviewForm) verdictButton(label string, id int, selected bool, on color.Color) string {
	t := f.deps.Styles.Theme
	borderCol, textCol := t.Primary, t.Muted
	if selected {
		borderCol, textCol = on, on
	}
	if f.focus == id {
		borderCol = t.Accent
	} else if f.hover == id {
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(selected).Render(label)
	return zone.Mark(reviewZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f reviewForm) buttons() string {
	var group []string
	if f.canReview {
		label := "Submit"
		if f.sending {
			label = "Submitting…"
		}
		group = append(group, f.button(label, rvSubmit))
	}
	if len(f.rerequestIDs) > 0 {
		group = append(group, f.button("Re-request review", rvRerequest))
	}
	group = append(group, f.button("Cancel", rvCancel))

	spaced := make([]string, 0, len(group)*2)
	for i, b := range group {
		if i > 0 {
			spaced = append(spaced, " ")
		}
		spaced = append(spaced, b)
	}
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, lipgloss.JoinHorizontal(lipgloss.Top, spaced...))
}

func (f reviewForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(reviewZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f reviewForm) field(id int, label, content string) string {
	t := f.deps.Styles.Theme
	borderCol := t.Primary
	switch {
	case f.focus == id:
		borderCol = t.Accent
	case f.hover == id:
		borderCol = t.Secondary
	}
	return zone.Mark(reviewZone(id), components.TitledBoxWeighted(label, content, borderCol, f.focus == id))
}

// FocusRow reports the focused control's row and height, so a windowed modal scrolls to keep it visible.
// chromeTop = top border + the padding row above the body.
func (f reviewForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	if !f.canReview {
		return 0, 0, false // the short read-only variant always fits
	}
	verdict := f.verdictBar()
	field := f.field(rvFeedback, "Feedback", fixField(f.feedback.View(), reviewFeedbackH))
	head := chromeTop + 1 // the "Your verdict" label
	switch f.focus {
	case rvApprove, rvReject:
		return head, lipgloss.Height(verdict), true
	case rvFeedback:
		return head + lipgloss.Height(verdict) + 1, lipgloss.Height(field), true
	}
	return head + lipgloss.Height(verdict) + 1 + lipgloss.Height(field), lipgloss.Height(f.buttons()), true
}

func (f reviewForm) HelpKeys() []key.Binding {
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	if f.focus == rvApprove || f.focus == rvReject {
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "verdict")))
	}
	return append(binds,
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	)
}

func reviewZone(id int) string {
	switch id {
	case rvApprove:
		return "project.review.approve"
	case rvReject:
		return "project.review.reject"
	case rvFeedback:
		return "project.review.feedback"
	case rvSubmit:
		return "project.review.submit"
	case rvRerequest:
		return "project.review.rerequest"
	case rvCancel:
		return "project.review.cancel"
	}
	return ""
}

type reviewSubmittedMsg struct {
	approved bool
	comment  string
}

type reviewRerequestMsg struct{}

type reviewCancelledMsg struct{}

func cancelReview() tea.Msg { return reviewCancelledMsg{} }

func requestReReview() tea.Msg { return reviewRerequestMsg{} }

func submitReview(approved bool, comment string) tea.Cmd {
	return func() tea.Msg { return reviewSubmittedMsg{approved: approved, comment: comment} }
}

// ReviewDoneMsg is exported so the app shell can route this result back after the user left the drill-in.
type ReviewDoneMsg struct {
	key     string
	err     bool
	errText string
	text    string // the success toast line
}

func submitReviewCmd(d deps.Deps, key string, approved bool, comment string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Issues.SubmitReview(context.Background(), key, approved, comment); err != nil {
			return ReviewDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not submit the review.")}
		}
		text := "Review submitted: approved."
		if !approved {
			text = "Review submitted: changes requested."
		}
		return ReviewDoneMsg{key: key, text: text}
	}
}

func requestReviewCmd(d deps.Deps, key string, memberIDs []int64) tea.Cmd {
	return func() tea.Msg {
		if err := d.Issues.RequestReview(context.Background(), key, memberIDs); err != nil {
			return ReviewDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not request a review.")}
		}
		return ReviewDoneMsg{key: key, text: "Review requested again."}
	}
}

// openReviewForm refuses when the caller has neither a verdict to give nor a reviewer to ask again.
func (m Model) openReviewForm() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	canReview := false
	if it, found := m.selectedIssue(); found {
		canReview = it.MyReviewStatus != ""
	}
	ids, names := respondedReviewers(d.Reviewers)
	if !canReview && len(ids) == 0 {
		return m, toast.Show(toast.Info, "You are not a reviewer on this issue.")
	}

	m.reviewing = true
	m.reviewScroll = 0
	m.reviewUI = newReviewForm(m.deps, canReview, ids, names)
	return m, m.reviewUI.Init()
}

// respondedReviewers are the only ones a re-request can reset. Resetting a PENDING one just notifies them.
func respondedReviewers(reviewers []domain.Reviewer) (ids []int64, names []string) {
	for _, rv := range reviewers {
		if rv.Status != "" && rv.Status != "PENDING" {
			ids = append(ids, rv.MemberID)
			names = append(names, orDash(rv.Name))
		}
	}
	return ids, names
}

// updateReview keeps the modal up until the result lands, so a failure reason can show in place.
func (m Model) updateReview(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case reviewCancelledMsg:
		m.reviewing = false
		return m, nil
	case reviewSubmittedMsg:
		return m, submitReviewCmd(m.deps, m.viewKey, msg.approved, msg.comment)
	case reviewRerequestMsg:
		m.reviewUI.sending = true
		return m, requestReviewCmd(m.deps, m.viewKey, m.reviewUI.rerequestIDs)
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.reviewUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.reviewScroll = clampScroll(m.reviewScroll-1, m.reviewScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.reviewScroll = clampScroll(m.reviewScroll+1, m.reviewScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.reviewUI, cmd = m.reviewUI.Update(msg)
	return m.followReviewFocus(), cmd
}

func (m Model) reviewScrollMax() int {
	return max(0, lipgloss.Height(m.reviewUI.View())-m.height)
}

func (m Model) followReviewFocus() Model {
	row, height, ok := m.reviewUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.reviewUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2
	off := m.reviewScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.reviewScroll = min(max(off, 0), boxH-m.height)
	return m
}
