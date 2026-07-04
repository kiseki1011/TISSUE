from __future__ import annotations

from textual import on
from textual.css.query import NoMatches
from textual.widgets import Input

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _SEARCH_DEBOUNCE


class IssueSearchMixin(ProjectHomeBase):
    """Search box behavior for the issue lists (issues/agent/reviews) and members."""

    @on(Input.Changed, "#hub-search")
    def _on_search_changed(self) -> None:
        self._cancel_search_timer()
        if self._ui.view_mode == "sprints":
            return
        self._search_timer = self.set_timer(_SEARCH_DEBOUNCE, self._run_search)

    @on(Input.Submitted, "#hub-search")
    def _on_search_submitted(self) -> None:
        self._cancel_search_timer()
        self._run_search()

    def _run_search(self) -> None:
        try:
            keyword = self.query_one("#hub-search", Input).value.strip() or None
        except NoMatches:
            return
        if self._ui.view_mode == "members":
            self.run_worker(
                self._render_members_list(keyword), exclusive=True, group="hub-list"
            )
        elif self._ui.view_mode in ("agent", "reviews"):
            self.run_worker(
                self._load_agent_issues(keyword), exclusive=True, group="hub-list"
            )
        elif self._ui.view_mode == "issues":
            self.run_worker(
                self._load_issues(keyword), exclusive=True, group="hub-list"
            )

    def _cancel_search_timer(self) -> None:
        if self._search_timer is not None:
            self._search_timer.stop()
            self._search_timer = None

    def _update_search_input(self) -> None:
        try:
            search_input = self.query_one("#hub-search", Input)
        except NoMatches:
            return
        if self._ui.view_mode == "sprints":
            search_input.disabled = True
            search_input.placeholder = "Search unavailable for sprints"
        else:
            search_input.disabled = False
            search_input.placeholder = (
                "Search project members…"
                if self._ui.view_mode == "members"
                else "Search issues…"
            )

    def action_focus_search(self) -> None:
        self.query_one("#hub-search", Input).focus()

    def action_leave_search(self) -> None:
        focused = self.app.focused
        if focused is not None and focused.id == "hub-search":
            self._cancel_search_timer()
            self.action_focus_issues()
