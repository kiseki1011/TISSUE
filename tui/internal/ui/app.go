package ui

import (
	tea "charm.land/bubbletea/v2"
)

// App is the root model, currently a skeleton.
// Will grow into a screen router.
type App struct {
	serverURL string
	width     int
	height    int
}

// New builds the root model for the given server URL
func New(serverURL string) App {
	return App{serverURL: serverURL}
}

func (a App) Init() tea.Cmd {
	return nil
}

func (a App) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "q", "ctrl+c":
			return a, tea.Quit
		}
	case tea.WindowSizeMsg:
		a.width, a.height = msg.Width, msg.Height
	}
	return a, nil
}

func (a App) View() tea.View {
	body := "tissue TUI — skeleton\n\n" +
		"server: " + a.serverURL + "\n\n" +
		"press q to quit"
	v := tea.NewView(body)
	v.AltScreen = true
	return v
}
