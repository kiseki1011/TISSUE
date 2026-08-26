package project

import (
	"log/slog"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// initialFilter is the project's saved filter (the search keyword is not stored), else open issues.
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

// rememberFilter persists the filter (minus the keyword) so re-opening the project restores it. Best-effort.
func (m Model) rememberFilter() {
	if m.deps.Config == nil {
		return
	}
	if err := m.deps.Config.SetProjectFilter(m.deps.Server, m.projectKey, filterState(m.filter)); err != nil {
		slog.Warn("save project filter", "key", m.projectKey, "err", err)
	}
}
