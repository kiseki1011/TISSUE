from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.css.query import NoMatches
from textual.widgets import Button, Input

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.issue_filter import DEFAULT_ISSUE_FILTER
from tissue.screens.project_home.issue_filter_modal import IssueFilterModal

if TYPE_CHECKING:
    from tissue.screens.project_home.issue_filter import IssueFilter


class FilterMixin(ProjectHomeBase):
    """The ⚙ filter button beside the search bar: opens the filter modal and applies
    the chosen `IssueFilter` to the [1] Issues list (and, when opted in, [3] Agent
    Work). The active filter lives on the screen (`self._filter`); the ⚙ button is
    accented while it differs from the default."""

    @on(Button.Pressed, "#hub-filter")
    def _on_filter_pressed(self) -> None:
        self.run_worker(self._open_filter_modal(), exclusive=True, group="hub-filter")

    async def _open_filter_modal(self) -> None:
        # The Sprint picker needs the sprint list and the Assignee picker the member
        # roster; load both quietly (no list render) in case the user opened the
        # filter before the Issues view finished loading them.
        await self._ensure_sprints_loaded()
        await self._ensure_members_loaded()
        self.app.push_screen(
            IssueFilterModal(
                current=self._filter,
                members=self._members,
                sprints=self._sprints,
            ),
            self._on_filter_applied,
        )

    def _on_filter_applied(self, new_filter: IssueFilter | None) -> None:
        if new_filter is None:
            return  # cancelled
        self._filter = new_filter
        self._update_filter_button()
        # Drop any pending live-search debounce: otherwise it fires after this load
        # and re-runs in the shared 'hub-list' group, cancelling the filtered load.
        self._cancel_search_timer()
        # The filter narrows the issue list, so surface it: switch to the Issues view
        # if we're elsewhere (clears the keyword, as a view switch always does),
        # otherwise keep the current keyword and just reload.
        switching = self._view_mode != "issues"
        if switching:
            self._set_view_chrome("issues")
        keyword = None if switching else self._search_keyword()
        self.run_worker(self._load_issues(keyword), exclusive=True, group="hub-list")
        # Agent Work reloads either way: a turned-on `apply_to_agent` narrows it, a
        # turned-off one restores it.
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def _update_filter_button(self) -> None:
        """Accent the ⚙ button while the filter differs from the default, so a
        narrowed list never looks like the full one."""
        try:
            btn = self.query_one("#hub-filter", Button)
        except NoMatches:
            return
        active = self._filter != DEFAULT_ISSUE_FILTER
        btn.set_class(active, "-filter-active")
        btn.tooltip = "Filter (active)" if active else "Filter issues"

    def _search_keyword(self) -> str | None:
        try:
            return self.query_one("#hub-search", Input).value.strip() or None
        except NoMatches:
            return None
