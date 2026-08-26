package widgets

import (
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var aug15 = time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC)

func keyCode(c rune) tea.KeyPressMsg { return tea.KeyPressMsg{Code: c} }

// Arrows move the selection by a day (left/right) and a week (up/down).
func TestDatePickerDayNav(t *testing.T) {
	p := NewDatePicker("Due", aug15, false, true, 24)
	if d := p.Update(keyCode(tea.KeyLeft)).Value().Day(); d != 14 {
		t.Errorf("left -> day %d, want 14", d)
	}
	if d := p.Update(keyCode(tea.KeyRight)).Value().Day(); d != 16 {
		t.Errorf("right -> day %d, want 16", d)
	}
	if d := p.Update(keyCode(tea.KeyUp)).Value().Day(); d != 8 {
		t.Errorf("up -> day %d, want 8", d)
	}
	if d := p.Update(keyCode(tea.KeyDown)).Value().Day(); d != 22 {
		t.Errorf("down -> day %d, want 22", d)
	}
}

func TestDatePickerDayCrossesMonth(t *testing.T) {
	aug31 := time.Date(2026, 8, 31, 0, 0, 0, 0, time.UTC)
	got := NewDatePicker("Due", aug31, false, true, 24).Update(keyCode(tea.KeyRight)).Value()
	if got.Month() != time.September || got.Day() != 1 {
		t.Errorf("Aug 31 +1 day -> %s, want 2026-09-01", got.Format("2006-01-02"))
	}
}

// A month step clamps the day rather than skipping into the month after.
func TestDatePickerMonthClamp(t *testing.T) {
	jan31 := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)
	got := NewDatePicker("Due", jan31, false, true, 24).MoveMonth(1).Value()
	if got.Month() != time.February || got.Day() != 28 {
		t.Errorf("Jan 31 +1 month -> %s, want 2026-02-28", got.Format("2006-01-02"))
	}
}

// pgup/pgdown and [ ] step the month.
func TestDatePickerMonthNavKeys(t *testing.T) {
	p := NewDatePicker("Due", aug15, false, true, 24)
	if m := p.Update(keyRune(']')).Value().Month(); m != time.September {
		t.Errorf("] -> month %v, want September", m)
	}
	if m := p.Update(keyCode(tea.KeyPgUp)).Value().Month(); m != time.July {
		t.Errorf("pgup -> month %v, want July", m)
	}
	if m := p.Update(keyCode(tea.KeyPgDown)).Value().Month(); m != time.September {
		t.Errorf("pgdown -> month %v, want September", m)
	}
}

// tab moves focus into the time steppers. up/down adjust the focused segment.
func TestDatePickerTimeStepper(t *testing.T) {
	init := time.Date(2026, 8, 15, 14, 30, 0, 0, time.Local)
	p := NewDatePicker("When", init, true, false, 24)
	p = p.Update(keyCode(tea.KeyTab)).Update(keyCode(tea.KeyUp)) // hour 14 -> 15
	if h := p.Value().Hour(); h != 15 {
		t.Errorf("hour after tab+up = %d, want 15", h)
	}
	p = p.Update(keyCode(tea.KeyTab)).Update(keyCode(tea.KeyDown)) // minute 30 -> 29
	if mi := p.Value().Minute(); mi != 29 {
		t.Errorf("minute after tab+down = %d, want 29", mi)
	}
}

// The hour stepper wraps 23 -> 0 and leaves the date untouched.
func TestDatePickerHourWraps(t *testing.T) {
	init := time.Date(2026, 8, 15, 23, 0, 0, 0, time.Local)
	p := NewDatePicker("When", init, true, false, 24).Update(keyCode(tea.KeyTab)) // focus hour
	got := p.Update(keyCode(tea.KeyUp)).Value()
	if got.Hour() != 0 || got.Day() != 15 {
		t.Errorf("hour 23 +1 -> %02d:xx on day %d, want 00 on day 15", got.Hour(), got.Day())
	}
}

// Value is always UTC: midnight when date-only, the picked wall-clock when timed.
func TestDatePickerValueZones(t *testing.T) {
	dateOnly := NewDatePicker("Due", aug15, false, true, 24).Value()
	if dateOnly.Location() != time.UTC || dateOnly.Hour() != 0 || dateOnly.Minute() != 0 {
		t.Errorf("date-only value should be UTC midnight, got %v (%v)", dateOnly, dateOnly.Location())
	}
	timed := NewDatePicker("When", time.Date(2026, 8, 15, 9, 5, 0, 0, time.Local), true, false, 24).Value()
	if timed.Location() != time.UTC || timed.Hour() != 9 || timed.Minute() != 5 {
		t.Errorf("timed value should carry the wall-clock in UTC components, got %v (%v)", timed, timed.Location())
	}
}

// A zero initial defaults to today.
func TestDatePickerDefaultsToToday(t *testing.T) {
	now := time.Now()
	v := NewDatePicker("Due", time.Time{}, false, true, 24).Value()
	if v.Year() != now.Year() || v.Month() != now.Month() || v.Day() != now.Day() {
		t.Errorf("zero initial should default to today, got %s", v.Format("2006-01-02"))
	}
}

func TestDatePickerViewRendersMonth(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	view := zone.Scan(NewDatePicker("Due date", aug15, false, true, 24).View(s))
	for _, want := range []string{"Due date", "August 2026", "Su", "15", "OK", "Clear"} {
		if !strings.Contains(view, want) {
			t.Errorf("view missing %q:\n%s", want, view)
		}
	}
}

func TestDatePickerViewTimeAndNoClear(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	view := zone.Scan(NewDatePicker("When", time.Date(2026, 8, 15, 14, 30, 0, 0, time.Local), true, false, 24).View(s))
	if !strings.Contains(view, "14") || !strings.Contains(view, "30") {
		t.Errorf("timed view should show the HH:MM row:\n%s", view)
	}
	if strings.Contains(view, "Clear") {
		t.Errorf("a non-clearable picker should not render Clear:\n%s", view)
	}
}

func TestDatePickerClicks(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewDatePicker("Due", aug15, false, true, 24)

	settleZone(t, p.View(s), dpDayZone(20))
	z := zone.Get(dpDayZone(20))
	if got, ok := p.HitDay(tea.MouseClickMsg{X: z.StartX, Y: z.StartY, Button: tea.MouseLeft}); !ok || got.Day() != 20 {
		t.Fatalf("HitDay = %v ok=%v, want day 20", got, ok)
	}

	settleZone(t, p.View(s), dpNextZone)
	nz := zone.Get(dpNextZone)
	if delta, ok := p.HitMonthNav(tea.MouseClickMsg{X: nz.StartX, Y: nz.StartY, Button: tea.MouseLeft}); !ok || delta != 1 {
		t.Fatalf("HitMonthNav next = %d ok=%v, want +1", delta, ok)
	}
}
