package project

import (
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// openPeek raises the read-only peek modal for a linked issue (a parent/child/relation), loading its
// detail into the shared cache when it is not already there (or in flight). The load is cache-aware and
// deduped like a selection; the peek body reads from the cache each render, so a landing load fills it in.
func (m Model) openPeek(key string) (Model, tea.Cmd) {
	if key == "" {
		return m, nil
	}
	m.peeking = true
	m.peekKey = key
	m.peekScroll = 0
	if _, cached := m.details[key]; !cached && !m.detailsPending[key] {
		return m, m.startDetailLoad(key)
	}
	return m, nil
}

// closePeek dismisses the peek modal, returning to the issue that opened it.
func (m Model) closePeek() (Model, tea.Cmd) {
	m.peeking = false
	m.peekKey = ""
	m.peekScroll = 0
	return m, nil
}

// updatePeek owns input while the peek modal is open: esc closes it, the scroll keys and the wheel move
// its window. It is read-only, so every other key (and any click) is swallowed.
func (m Model) updatePeek(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "esc":
			return m.closePeek()
		case "up", "k":
			m.peekScroll = clampScroll(m.peekScroll-1, m.peekScrollMax())
		case "down", "j":
			m.peekScroll = clampScroll(m.peekScroll+1, m.peekScrollMax())
		case "pgup":
			m.peekScroll = clampScroll(m.peekScroll-m.peekPage(), m.peekScrollMax())
		case "pgdown", "space":
			m.peekScroll = clampScroll(m.peekScroll+m.peekPage(), m.peekScrollMax())
		case "home", "g":
			m.peekScroll = 0
		case "end", "G":
			m.peekScroll = m.peekScrollMax()
		case "r", "R":
			if m.detailsFailed[m.peekKey] { // recover a failed peek load in place, like the main panel's R
				return m, m.startDetailLoad(m.peekKey)
			}
		}
		return m, nil
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.peekModal()) <= m.height {
			return m, nil
		}
		m.peekScroll = wheelClamp(m.peekScroll, msg.Button, m.peekScrollMax())
		return m, nil
	}
	return m, nil
}

// hitPeek reports the linked issue whose key cell the click landed on, scanning the viewed issue's parent,
// children and relations. Checked before the panel zone (which spans the whole Details area and would
// otherwise swallow the click as a focus), mirroring hitCommentReply.
func (m Model) hitPeek(msg tea.MouseMsg) (string, bool) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return "", false
	}
	if p := parentKeyOf(d); p != "" && zone.Get(peekZone("p", p)).InBounds(msg) {
		return p, true
	}
	for _, c := range d.Children {
		if c.Key != "" && zone.Get(peekZone("c", c.Key)).InBounds(msg) {
			return c.Key, true
		}
	}
	for _, gr := range d.Relations {
		for _, it := range gr.Items {
			if it.Key != "" && zone.Get(peekZone("r", it.Key)).InBounds(msg) {
				return it.Key, true
			}
		}
	}
	return "", false
}

// hoverPeekZone reports the peek zone id under the cursor (for the hover highlight), scanning the same
// links hitPeek does but returning the section-qualified zone rather than the key.
func (m Model) hoverPeekZone(msg tea.MouseMsg) (string, bool) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return "", false
	}
	if p := parentKeyOf(d); p != "" {
		if z := peekZone("p", p); zone.Get(z).InBounds(msg) {
			return z, true
		}
	}
	for _, c := range d.Children {
		if c.Key != "" {
			if z := peekZone("c", c.Key); zone.Get(z).InBounds(msg) {
				return z, true
			}
		}
	}
	for _, gr := range d.Relations {
		for _, it := range gr.Items {
			if it.Key != "" {
				if z := peekZone("r", it.Key); zone.Get(z).InBounds(msg) {
					return z, true
				}
			}
		}
	}
	return "", false
}
