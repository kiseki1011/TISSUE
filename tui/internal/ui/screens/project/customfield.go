package project

import (
	"strconv"
	"strings"
	"time"

	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// customFieldInput is one custom field's input in the create form. It adapts to the field's type: a
// single-line text input for the text/number/date types, a cycle for BOOLEAN/SELECT_OPTION, and a
// left/right + space multi-toggle for CHECKLIST. value() serializes it to the backend's per-type JSON
// shape (number / string / bool / {optionId: bool}); an unset optional field contributes nothing.
type customFieldInput struct {
	field domain.IssueField

	text    textinput.Model // SHORT_TEXT/number types (single line)
	area    textarea.Model  // TEXT type (multi-line, like the Content field)
	date    time.Time       // DATE / TIMESTAMP value from the calendar, always UTC-stored: DATE is a UTC-midnight date; TIMESTAMP's components are the local wall-clock (localized on serialize)
	dateSet bool            // whether a DATE/TIMESTAMP value has been chosen
	ix      int             // BOOLEAN: 0 unset, 1 Yes, 2 No.  SELECT_OPTION: 0 unset, 1..N -> options[i-1]
	checked map[int]bool    // CHECKLIST: option id -> checked
	cursor  int             // CHECKLIST: highlighted option (moved with left/right, toggled with space)

	err string
}

// cfIsText reports the single-line text-input types. DATE/TIMESTAMP are excluded: they are chosen from
// the calendar picker (cfIsDate), and TEXT is a multi-line area (cfIsArea).
func cfIsText(fieldType string) bool {
	switch fieldType {
	case "SHORT_TEXT", "INTEGER", "DECIMAL", "PERCENTAGE":
		return true
	}
	return false
}

// cfIsArea is the subset of text types that use a multi-line textarea rather than a single-line input:
// only TEXT (long free text), rendered like the Content field.
func cfIsArea(fieldType string) bool { return fieldType == "TEXT" }

// cfIsDate reports the date/time types, chosen from the calendar picker rather than typed.
func cfIsDate(fieldType string) bool { return fieldType == "DATE" || fieldType == "TIMESTAMP" }

func (c customFieldInput) isArea() bool { return cfIsArea(c.field.Type) }
func (c customFieldInput) isDate() bool { return cfIsDate(c.field.Type) }

func cfIsCycle(fieldType string) bool { return fieldType == "BOOLEAN" || fieldType == "SELECT_OPTION" }

func cfIsChecklist(fieldType string) bool { return fieldType == "CHECKLIST" }

func newCustomFieldInput(f domain.IssueField) customFieldInput {
	c := customFieldInput{field: f, checked: map[int]bool{}}
	switch {
	case cfIsArea(f.Type):
		ta := textarea.New()
		ta.Prompt = ""
		ta.ShowLineNumbers = false
		ta.CharLimit = 0 // TEXT is unbounded, like the Content body
		ta.SetWidth(editFieldW)
		ta.SetHeight(editContentH)
		c.area = ta
	case cfIsText(f.Type):
		ti := textinput.New()
		ti.Prompt = ""
		ti.SetWidth(editFieldW)
		ti.CharLimit = cfCharLimit(f.Type)
		ti.Placeholder = cfPlaceholder(f.Type)
		c.text = ti
	}
	return c
}

func cfCharLimit(fieldType string) int {
	switch fieldType {
	case "SHORT_TEXT":
		return 255
	default:
		return 32 // numbers
	}
}

func cfPlaceholder(fieldType string) string {
	switch fieldType {
	case "INTEGER", "DECIMAL":
		return "number"
	case "PERCENTAGE":
		return "0-100"
	default:
		return "Optional"
	}
}

func (c customFieldInput) focus() (customFieldInput, tea.Cmd) {
	switch {
	case cfIsArea(c.field.Type):
		return c, c.area.Focus()
	case cfIsText(c.field.Type):
		return c, c.text.Focus()
	}
	return c, nil // a date/cycle/checklist field has no text cursor to focus
}

func (c customFieldInput) blur() customFieldInput {
	switch {
	case cfIsArea(c.field.Type):
		c.area.Blur()
	case cfIsText(c.field.Type):
		c.text.Blur()
	}
	return c
}

// setDate records a calendar pick (set=false clears an optional field). The value's zone is the
// picker's: UTC midnight for DATE, local wall-clock for TIMESTAMP.
func (c customFieldInput) setDate(v time.Time, set bool) customFieldInput {
	c.date, c.dateSet, c.err = v, set, ""
	return c
}

// dateInitial seeds the calendar picker: the chosen value, or zero so the picker defaults to today/now.
func (c customFieldInput) dateInitial() time.Time {
	if c.dateSet {
		return c.date
	}
	return time.Time{}
}

// handleKey processes a key while this field is focused, returning the updated field and whether it
// consumed the key (so the form does not also treat it as navigation).
func (c customFieldInput) handleKey(msg tea.KeyPressMsg) (customFieldInput, bool) {
	switch {
	case cfIsArea(c.field.Type):
		c.err = ""
		c.area, _ = c.area.Update(msg) // a textarea: enter inserts a newline, up/down move the cursor
		return c, true
	case cfIsText(c.field.Type):
		c.err = ""
		c.text, _ = c.text.Update(msg)
		return c, true
	case cfIsCycle(c.field.Type):
		switch msg.String() {
		case "left":
			c.ix = (c.ix - 1 + c.cycleLen()) % c.cycleLen()
			return c, true
		case "right", "space":
			c.ix = (c.ix + 1) % c.cycleLen()
			return c, true
		}
	case cfIsChecklist(c.field.Type):
		switch msg.String() {
		case "left":
			if n := len(c.field.Options); n > 0 {
				c.cursor = (c.cursor - 1 + n) % n
			}
			return c, true
		case "right":
			if n := len(c.field.Options); n > 0 {
				c.cursor = (c.cursor + 1) % n
			}
			return c, true
		case "space":
			if c.cursor >= 0 && c.cursor < len(c.field.Options) {
				id := c.field.Options[c.cursor].ID
				c.checked[id] = !c.checked[id]
			}
			return c, true
		}
	}
	return c, false
}

// cycleLen is the number of states in a cycle field: BOOLEAN has 3 (unset/Yes/No), SELECT_OPTION has
// 1 (unset) + one per option.
func (c customFieldInput) cycleLen() int {
	if c.field.Type == "BOOLEAN" {
		return 3
	}
	return len(c.field.Options) + 1
}

// content renders the input body (without the surrounding labelled box, which the form adds). prefix is
// the field box's zone id, which the clickable sub-controls (cycle arrows, checklist options) extend so a
// click can land on one option rather than only on the field as a whole.
func (c customFieldInput) content(t theme.Theme, focused bool, prefix string) string {
	switch {
	case cfIsArea(c.field.Type):
		return fixField(c.area.View(), editContentH)
	case cfIsText(c.field.Type):
		return fixField(c.text.View(), 1)
	case cfIsDate(c.field.Type):
		return fixField(c.dateContent(t, focused), 1)
	case cfIsCycle(c.field.Type):
		arrow := t.Muted
		if focused {
			arrow = t.Accent
		}
		val := lipgloss.NewStyle().Foreground(t.Text).Render(c.cycleLabel())
		prev := zone.Mark(prefix+".prev", lipgloss.NewStyle().Foreground(arrow).Render("‹ "))
		next := zone.Mark(prefix+".next", lipgloss.NewStyle().Foreground(arrow).Render(" ›"))
		return fixField(prev+val+next, 1)
	case cfIsChecklist(c.field.Type):
		return c.checklistBody(t, focused, prefix)
	}
	return fixField("", 1)
}

// dateContent renders the DATE/TIMESTAMP field body: the chosen value, or a "Select…" hint. Accent when
// focused, mirroring the Parent field's picker-backed look.
func (c customFieldInput) dateContent(t theme.Theme, focused bool) string {
	col := t.Muted
	label := "Select…"
	if c.dateSet {
		label, col = c.dateDisplay(), t.Text
	}
	if focused {
		col = t.Accent
	}
	return lipgloss.NewStyle().Foreground(col).Render(label)
}

// dateDisplay is the human-readable chosen value: a date for DATE, a date-time for TIMESTAMP. date is
// UTC-stored, so Format (in UTC) prints its components directly - which for TIMESTAMP are the intended
// local wall-clock.
func (c customFieldInput) dateDisplay() string {
	if c.field.Type == "TIMESTAMP" {
		return c.date.Format("2006-01-02 15:04")
	}
	return c.date.Format("2006-01-02")
}

func (c customFieldInput) cycleLabel() string {
	if c.ix == 0 {
		return "-"
	}
	if c.field.Type == "BOOLEAN" {
		if c.ix == 1 {
			return "Yes"
		}
		return "No"
	}
	if i := c.ix - 1; i >= 0 && i < len(c.field.Options) {
		return c.field.Options[i].Name
	}
	return "-"
}

func (c customFieldInput) checklistBody(t theme.Theme, focused bool, prefix string) string {
	if len(c.field.Options) == 0 {
		return fixField(lipgloss.NewStyle().Foreground(t.Muted).Render("(no options)"), 1)
	}
	rows := make([]string, len(c.field.Options))
	for i, o := range c.field.Options {
		box := "[ ]"
		style := lipgloss.NewStyle().Foreground(t.Muted)
		if c.checked[o.ID] {
			box = "[x]"
			style = lipgloss.NewStyle().Foreground(t.Success)
		}
		name := flattenLine(o.Name)
		if focused && i == c.cursor {
			name = lipgloss.NewStyle().Foreground(t.Accent).Render(name)
		} else {
			name = lipgloss.NewStyle().Foreground(t.Text).Render(name)
		}
		rows[i] = zone.Mark(prefix+".opt."+strconv.Itoa(i), style.Render(box)+" "+name)
	}
	body := lipgloss.JoinVertical(lipgloss.Left, rows...)
	return lipgloss.NewStyle().Width(editFieldW).MaxWidth(editFieldW).Render(body)
}

// seed fills the input from a stored value in the shape the backend returns for this field type, so an
// edit form opens on the issue's current values. It is the inverse of value(); a nil or unexpectedly
// shaped raw leaves the input unset rather than guessing.
func (c customFieldInput) seed(raw interface{}) customFieldInput {
	if raw == nil {
		return c
	}
	switch c.field.Type {
	case "TEXT":
		if v, ok := raw.(string); ok {
			c.area.SetValue(v)
		}
	case "SHORT_TEXT", "DECIMAL":
		if v, ok := raw.(string); ok {
			c.text.SetValue(v)
		}
	case "INTEGER", "PERCENTAGE":
		if v, ok := raw.(float64); ok { // JSON numbers decode to float64
			c.text.SetValue(strconv.Itoa(int(v)))
		}
	case "DATE":
		if v, ok := raw.(string); ok {
			if d, err := time.Parse("2006-01-02", v); err == nil {
				c.date, c.dateSet = d, true // parsed in UTC, which is where a date-only value lives
			}
		}
	case "TIMESTAMP":
		if v, ok := raw.(string); ok {
			if inst, err := time.Parse(time.RFC3339, v); err == nil {
				// store the local wall-clock components in UTC, the convention value() reads back
				l := inst.Local()
				c.date = time.Date(l.Year(), l.Month(), l.Day(), l.Hour(), l.Minute(), 0, 0, time.UTC)
				c.dateSet = true
			}
		}
	case "BOOLEAN":
		if v, ok := raw.(bool); ok {
			c.ix = 2
			if v {
				c.ix = 1
			}
		}
	case "SELECT_OPTION":
		if v, ok := raw.(float64); ok {
			for i, o := range c.field.Options {
				if o.ID == int(v) {
					c.ix = i + 1
					break
				}
			}
		}
	case "CHECKLIST":
		if m, ok := raw.(map[string]interface{}); ok {
			for _, o := range c.field.Options {
				if on, _ := m[strconv.Itoa(o.ID)].(bool); on {
					c.checked[o.ID] = true
				}
			}
		}
	}
	return c
}

// clickAt applies a click that landed inside this field's box. It reports whether a sub-control consumed
// it, so the caller does not also treat the click as a plain focus (or, for a date field, as a request to
// open the calendar). Keyboard and mouse stay in step: a toggled option also moves the cursor there.
func (c customFieldInput) clickAt(msg tea.MouseMsg, prefix string) (customFieldInput, bool) {
	switch {
	case cfIsChecklist(c.field.Type):
		for i := range c.field.Options {
			if inZone(prefix+".opt."+strconv.Itoa(i), msg) {
				c.cursor = i
				id := c.field.Options[i].ID
				c.checked[id] = !c.checked[id]
				c.err = ""
				return c, true
			}
		}
	case cfIsCycle(c.field.Type):
		switch {
		case inZone(prefix+".prev", msg):
			c.ix = (c.ix - 1 + c.cycleLen()) % c.cycleLen()
			c.err = ""
			return c, true
		case inZone(prefix+".next", msg):
			c.ix = (c.ix + 1) % c.cycleLen()
			c.err = ""
			return c, true
		}
	}
	return c, false
}

// inZone is zone.Get(...).InBounds guarded against an unregistered id, which Get reports as nil - a
// sub-control that has not been rendered yet (or was scrolled out) must read as "not hit", not panic.
func inZone(id string, msg tea.MouseMsg) bool {
	z := zone.Get(id)
	return z != nil && z.InBounds(msg)
}

// lines is how many rendered rows this input occupies, so the form can size its window.
func (c customFieldInput) lines() int {
	switch {
	case cfIsArea(c.field.Type):
		return editContentH
	case cfIsChecklist(c.field.Type):
		return max(1, len(c.field.Options))
	}
	return 1
}

// value serializes the input to the backend's per-type JSON shape. present is false for an unset optional
// field (nothing is sent); err is non-empty when the entry is invalid or a required field is missing.
func (c customFieldInput) value() (val interface{}, present bool, err string) {
	req := c.field.Required
	switch c.field.Type {
	case "SHORT_TEXT":
		s := strings.TrimSpace(c.text.Value())
		if s == "" {
			return nil, false, requiredErr(req)
		}
		return s, true, ""
	case "TEXT":
		s := strings.TrimSpace(c.area.Value())
		if s == "" {
			return nil, false, requiredErr(req)
		}
		return s, true, ""
	case "INTEGER":
		s := strings.TrimSpace(c.text.Value())
		if s == "" {
			return nil, false, requiredErr(req)
		}
		n, e := strconv.Atoi(s)
		if e != nil {
			return nil, false, "Whole number only"
		}
		return n, true, ""
	case "PERCENTAGE":
		s := strings.TrimSpace(c.text.Value())
		if s == "" {
			return nil, false, requiredErr(req)
		}
		n, e := strconv.Atoi(s)
		if e != nil || n < 0 || n > 100 {
			return nil, false, "0-100 only"
		}
		return n, true, ""
	case "DECIMAL":
		s := strings.TrimSpace(c.text.Value())
		if s == "" {
			return nil, false, requiredErr(req)
		}
		if _, e := strconv.ParseFloat(s, 64); e != nil {
			return nil, false, "Number only"
		}
		return s, true, "" // sent as a string so the backend's BigDecimal keeps exact precision
	case "DATE":
		if !c.dateSet {
			return nil, false, requiredErr(req)
		}
		return c.date.Format("2006-01-02"), true, "" // date is UTC midnight, so Format yields the calendar date
	case "TIMESTAMP":
		if !c.dateSet {
			return nil, false, requiredErr(req)
		}
		// date's components are the local wall-clock (UTC-stored for gap-free calendar math); read them
		// back as local and send the UTC instant, so it round-trips with the local-time display
		wall := c.date
		local := time.Date(wall.Year(), wall.Month(), wall.Day(), wall.Hour(), wall.Minute(), 0, 0, time.Local)
		return local.UTC().Format(time.RFC3339), true, ""
	case "BOOLEAN":
		if c.ix == 0 {
			return nil, false, requiredErr(req)
		}
		return c.ix == 1, true, ""
	case "SELECT_OPTION":
		if c.ix == 0 {
			return nil, false, requiredErr(req)
		}
		if i := c.ix - 1; i >= 0 && i < len(c.field.Options) {
			return int64(c.field.Options[i].ID), true, ""
		}
		return nil, false, requiredErr(req)
	case "CHECKLIST":
		out := map[string]bool{}
		for _, o := range c.field.Options {
			if c.checked[o.ID] {
				out[strconv.Itoa(o.ID)] = true
			}
		}
		if len(out) == 0 {
			return nil, false, requiredErr(req)
		}
		return out, true, ""
	}
	return nil, false, ""
}

func requiredErr(required bool) string {
	if required {
		return "Required field"
	}
	return ""
}

// cfHelp is the extra key hint for the focused custom field (beyond tab/enter), or "" for a text field.
func (c customFieldInput) cfHelp() string {
	switch {
	case cfIsDate(c.field.Type):
		return "enter calendar"
	case cfIsCycle(c.field.Type):
		return "←/→ change"
	case cfIsChecklist(c.field.Type):
		return "←/→ move · space toggle"
	}
	return ""
}

// label prefixes a required field's name with a marker.
func (c customFieldInput) label() string {
	if c.field.Required {
		return c.field.Name + " *"
	}
	return c.field.Name
}
