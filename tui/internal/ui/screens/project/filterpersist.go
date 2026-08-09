package project

import (
	"log/slog"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// initialFilter is the filter a freshly opened project starts with: the user's last-applied filter for
// that project when one is saved (the transient search keyword is not stored), else the default
// open-issues view.
func initialFilter(d deps.Deps, key string) domain.IssueFilter {
	if d.Config != nil {
		if fs, ok := d.Config.ProjectFilter(d.Server, key); ok {
			return filterFromState(fs)
		}
	}
	return domain.OpenIssuesFilter()
}

func filterFromState(s config.FilterState) domain.IssueFilter {
	return domain.IssueFilter{
		StateCategories:   s.StateCategories,
		Priorities:        s.Priorities,
		IssueTypeIDs:      s.IssueTypeIDs,
		SprintIDs:         s.SprintIDs,
		CurrentSprintOnly: s.CurrentSprintOnly,
		AssigneeMe:        s.AssigneeMe,
		AuthorMe:          s.AuthorMe,
		ReviewerMe:        s.ReviewerMe,
		ReviewerStatuses:  s.ReviewerStatuses,
	}
}

func filterState(f domain.IssueFilter) config.FilterState {
	return config.FilterState{
		StateCategories:   f.StateCategories,
		Priorities:        f.Priorities,
		IssueTypeIDs:      f.IssueTypeIDs,
		SprintIDs:         f.SprintIDs,
		CurrentSprintOnly: f.CurrentSprintOnly,
		AssigneeMe:        f.AssigneeMe,
		AuthorMe:          f.AuthorMe,
		ReviewerMe:        f.ReviewerMe,
		ReviewerStatuses:  f.ReviewerStatuses,
	}
}

// rememberFilter persists the project's current filter (minus the transient keyword) so re-opening the
// project - including after a restart - restores it. Best-effort, matching the app's other config saves.
func (m Model) rememberFilter() {
	if m.deps.Config == nil {
		return
	}
	if err := m.deps.Config.SetProjectFilter(m.deps.Server, m.projectKey, filterState(m.filter)); err != nil {
		slog.Warn("save project filter", "key", m.projectKey, "err", err)
	}
}
