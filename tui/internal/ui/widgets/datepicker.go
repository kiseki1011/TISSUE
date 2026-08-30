package widgets

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// dpFocus is which segment the keyboard drives: the calendar grid, or the hour/minute stepper.
type dpFocus int

const (
	dpCalendar dpFocus = iota
	dpHour
	dpMinute
)

// dpGridW is the day grid's fixed width: 7 columns of a 2-wide number joined by single spaces.
const dpGridW = 7*2 + 6

const weekdayHeader = "Su Mo Tu We Th Fr Sa"

// DatePicker is a month-grid calendar with an optional HH:MM stepper. sel is always UTC so date and time
// arithmetic never crosses a DST gap. Date-only, that midnight is the value. Timed, it is wall-clock.
type DatePicker struct {
	title      string
	sel        time.Time
	now        time.Time // captured once at open so the "today" marker is stable across a session
	withTime   bool
	allowClear bool
	focus      dpFocus
	width      int
}

// NewDatePicker seeds from initial, or today when it is zero. allowClear offers an "unset" affordance.
func NewDatePicker(title string, initial time.Time, withTime, allowClear bool, width int) DatePicker {
	now := time.Now()
	base := initial
	if base.IsZero() {
		base = now
	}
	y, mo, d := base.Date()
	h, mi := 0, 0
	if withTime {
		h, mi = base.Hour(), base.Minute() // local wall-clock, whether base is now or a reopened value
	}
	sel := time.Date(y, mo, d, h, mi, 0, 0, time.UTC) // always UTC: gap-free arithmetic (see the type doc)
	return DatePicker{title: title, sel: sel, now: now, withTime: withTime, allowClear: allowClear, width: width}
}

// Value is always UTC: midnight when date-only, else the local wall-clock in its UTC components.
func (p DatePicker) Value() time.Time { return p.sel }

func (p DatePicker) WithTime() bool   { return p.withTime }
func (p DatePicker) AllowClear() bool { return p.allowClear }

// SetSel replaces the selection after a day click (HitDay already carries the current time-of-day).
func (p DatePicker) SetSel(t time.Time) DatePicker {
	p.sel = t
	return p
}

// Update handles a key while the picker is open. The host owns esc/enter and the clear key.
func (p DatePicker) Update(msg tea.KeyPressMsg) DatePicker {
	if p.withTime { // tab cycles calendar -> hour -> minute -> calendar
		switch msg.String() {
		case "tab":
			p.focus = (p.focus + 1) % 3
			return p
		case "shift+tab":
			p.focus = (p.focus + 2) % 3
			return p
		}
	}
	if p.focus == dpHour || p.focus == dpMinute {
		return p.updateTime(msg)
	}
	return p.updateCalendar(msg)
}

func (p DatePicker) updateCalendar(msg tea.KeyPressMsg) DatePicker {
	switch msg.String() {
	case "left":
		p.sel = p.sel.AddDate(0, 0, -1)
	case "right":
		p.sel = p.sel.AddDate(0, 0, 1)
	case "up":
		p.sel = p.sel.AddDate(0, 0, -7)
	case "down":
		p.sel = p.sel.AddDate(0, 0, 7)
	case "pgup", "[":
		p = p.moveMonth(-1)
	case "pgdown", "]":
		p = p.moveMonth(1)
	}
	return p
}

func (p DatePicker) updateTime(msg tea.KeyPressMsg) DatePicker {
	y, mo, d := p.sel.Date()
	h, mi := p.sel.Hour(), p.sel.Minute()
	switch msg.String() {
	case "up":
		if p.focus == dpHour {
			h = (h + 1) % 24
		} else {
			mi = (mi + 1) % 60
		}
	case "down":
		if p.focus == dpHour {
			h = (h + 23) % 24
		} else {
			mi = (mi + 59) % 60
		}
	case "left":
		p.focus = dpHour
		return p
	case "right":
		p.focus = dpMinute
		return p
	default:
		return p
	}
	p.sel = time.Date(y, mo, d, h, mi, 0, 0, p.sel.Location())
	return p
}

// MoveMonth clamps the day to the target month's length (Jan 31 -> Feb 28, not a skip into March).
func (p DatePicker) MoveMonth(delta int) DatePicker { return p.moveMonth(delta) }

func (p DatePicker) moveMonth(delta int) DatePicker {
	y, mo, d := p.sel.Date()
	first := time.Date(y, mo+time.Month(delta), 1, 0, 0, 0, 0, p.sel.Location())
	if maxD := daysIn(first.Year(), first.Month()); d > maxD {
		d = maxD
	}
	p.sel = time.Date(first.Year(), first.Month(), d, p.sel.Hour(), p.sel.Minute(), 0, 0, p.sel.Location())
	return p
}

// daysIn is the number of days in a month (day 0 of the next month is the last day of this one).
func daysIn(year int, month time.Month) int {
	return time.Date(year, month+1, 0, 0, 0, 0, 0, time.UTC).Day()
}

func dpDayZone(d int) string { return "widgets.datepicker.day." + strconv.Itoa(d) }

const (
	dpPrevZone   = "widgets.datepicker.prevmonth"
	dpNextZone   = "widgets.datepicker.nextmonth"
	dpHourZone   = "widgets.datepicker.hour"
	dpMinuteZone = "widgets.datepicker.minute"
	dpOKZone     = "widgets.datepicker.ok"
	dpClearZone  = "widgets.datepicker.clear"
)

// HitDay resolves a click to its day cell, keeping the picker's current HH:MM.
func (p DatePicker) HitDay(msg tea.MouseMsg) (time.Time, bool) {
	total := daysIn(p.sel.Year(), p.sel.Month())
	for d := 1; d <= total; d++ {
		if z := zone.Get(dpDayZone(d)); z != nil && z.InBounds(msg) {
			return time.Date(p.sel.Year(), p.sel.Month(), d, p.sel.Hour(), p.sel.Minute(), 0, 0, p.sel.Location()), true
		}
	}
	return time.Time{}, false
}

// HitMonthNav reports a click on the prev/next month arrows as -1/+1.
func (p DatePicker) HitMonthNav(msg tea.MouseMsg) (int, bool) {
	if z := zone.Get(dpPrevZone); z != nil && z.InBounds(msg) {
		return -1, true
	}
	if z := zone.Get(dpNextZone); z != nil && z.InBounds(msg) {
		return 1, true
	}
	return 0, false
}

// HitTimeSegment focuses the hour or minute stepper a click lands on. A no-op on a date-only picker.
func (p DatePicker) HitTimeSegment(msg tea.MouseMsg) (DatePicker, bool) {
	if !p.withTime {
		return p, false
	}
	if z := zone.Get(dpHourZone); z != nil && z.InBounds(msg) {
		p.focus = dpHour
		return p, true
	}
	if z := zone.Get(dpMinuteZone); z != nil && z.InBounds(msg) {
		p.focus = dpMinute
		return p, true
	}
	return p, false
}

// HitClear reports a click on the Clear affordance (only shown when allowClear).
func (p DatePicker) HitClear(msg tea.MouseMsg) bool {
	if !p.allowClear {
		return false
	}
	z := zone.Get(dpClearZone)
	return z != nil && z.InBounds(msg)
}

func (p DatePicker) HitConfirm(msg tea.MouseMsg) bool {
	z := zone.Get(dpOKZone)
	return z != nil && z.InBounds(msg)
}

func (p DatePicker) contentW() int {
	if p.width > dpGridW {
		return p.width
	}
	return dpGridW
}

func (p DatePicker) View(s theme.Styles) string {
	t := s.Theme
	y, mo, _ := p.sel.Date()
	loc := p.sel.Location()
	first := time.Date(y, mo, 1, 0, 0, 0, 0, loc)
	total := daysIn(y, mo)

	lines := []string{
		p.monthHeader(t),
		p.padCenter(lipgloss.NewStyle().Foreground(t.Muted).Render(weekdayHeader), lipgloss.Width(weekdayHeader)),
		"",
	}
	lines = append(lines, p.grid(t, first, total)...)
	if p.withTime {
		lines = append(lines, "", p.timeRow(t))
	}
	lines = append(lines, "", p.footer(t))

	body := lipgloss.JoinVertical(lipgloss.Left, lines...)
	return components.TitledBoxCentered(p.title, body, t.Primary)
}

// grid renders week rows. Blanks pad the first and last weeks so the box stays rectangular.
func (p DatePicker) grid(t theme.Theme, first time.Time, total int) []string {
	cells := make([]string, 0, 42)
	for i := 0; i < int(first.Weekday()); i++ {
		cells = append(cells, "  ")
	}
	for d := 1; d <= total; d++ {
		cells = append(cells, zone.Mark(dpDayZone(d), p.dayCell(t, d).Render(fmt.Sprintf("%2d", d))))
	}
	for len(cells)%7 != 0 {
		cells = append(cells, "  ")
	}
	rows := make([]string, 0, len(cells)/7)
	for i := 0; i < len(cells); i += 7 {
		rows = append(rows, p.padCenter(strings.Join(cells[i:i+7], " "), dpGridW))
	}
	return rows
}

// dayCell styles one day: selected is reversed (theme-robust), today underlined, the rest plain.
func (p DatePicker) dayCell(t theme.Theme, d int) lipgloss.Style {
	switch {
	case d == p.sel.Day():
		return lipgloss.NewStyle().Foreground(t.Accent).Reverse(true).Bold(true)
	case p.isToday(d):
		return lipgloss.NewStyle().Foreground(t.Text).Underline(true)
	default:
		return lipgloss.NewStyle().Foreground(t.Text)
	}
}

// isToday compares numeric Y/M/D, so the picker's UTC-vs-local sel zone cannot skew the marker.
func (p DatePicker) isToday(d int) bool {
	ny, nmo, nd := p.now.Date()
	return d == nd && p.sel.Month() == nmo && p.sel.Year() == ny
}

func (p DatePicker) monthHeader(t theme.Theme) string {
	label := p.sel.Format("January 2006")
	prev := zone.Mark(dpPrevZone, lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render("‹"))
	next := zone.Mark(dpNextZone, lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render("›"))
	mid := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(label)
	plainW := 1 + 2 + lipgloss.Width(label) + 2 + 1 // ‹ + gap + label + gap + ›
	return p.padCenter(prev+"  "+mid+"  "+next, plainW)
}

func (p DatePicker) timeRow(t theme.Theme) string {
	hh := fmt.Sprintf("%02d", p.sel.Hour())
	mm := fmt.Sprintf("%02d", p.sel.Minute())
	row := zone.Mark(dpHourZone, p.segStyle(t, dpHour).Render(hh)) +
		lipgloss.NewStyle().Foreground(t.Muted).Render(" : ") +
		zone.Mark(dpMinuteZone, p.segStyle(t, dpMinute).Render(mm))
	return p.padCenter(row, 2+3+2)
}

func (p DatePicker) segStyle(t theme.Theme, seg dpFocus) lipgloss.Style {
	if p.focus == seg {
		return lipgloss.NewStyle().Foreground(t.Accent).Reverse(true).Bold(true)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Bold(true)
}

// footer is the Clear (optional fields only) and OK affordances: Clear left, OK right.
func (p DatePicker) footer(t theme.Theme) string {
	ok := zone.Mark(dpOKZone, lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render("[ OK ]"))
	okW := lipgloss.Width("[ OK ]")
	if !p.allowClear {
		return p.padLeft(ok, okW, p.contentW()-okW)
	}
	clearBtn := zone.Mark(dpClearZone, lipgloss.NewStyle().Foreground(t.Muted).Render("Clear"))
	gap := p.contentW() - lipgloss.Width("Clear") - okW
	if gap < 1 {
		gap = 1
	}
	return clearBtn + strings.Repeat(" ", gap) + ok
}

// padCenter centers by the caller's plain width, since lipgloss.Width would count the zone markers.
func (p DatePicker) padCenter(s string, plainW int) string {
	if pad := (p.contentW() - plainW) / 2; pad > 0 {
		return strings.Repeat(" ", pad) + s
	}
	return s
}

func (p DatePicker) padLeft(s string, _, pad int) string {
	if pad > 0 {
		return strings.Repeat(" ", pad) + s
	}
	return s
}
