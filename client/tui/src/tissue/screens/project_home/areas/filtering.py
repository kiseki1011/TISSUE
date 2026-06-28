from __future__ import annotations

import logging

from textual import on
from textual.css.query import NoMatches
from textual.widgets import Button, Input

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.filter_persistence import (
    filter_from_dict,
    filter_to_dict,
)
from tissue.screens.project_home.issue_filter import (
    DEFAULT_ISSUE_FILTER,
    IssueFilter,
)
from tissue.screens.project_home.member_filter import (
    DEFAULT_MEMBER_FILTER,
    MemberFilter,
)
from tissue.screens.project_home.modals.issue_filter_modal import IssueFilterModal
from tissue.screens.project_home.modals.member_filter_modal import MemberFilterModal
from tissue.screens.project_home.modals.sprint_filter_modal import SprintFilterModal
from tissue.screens.project_home.sprint_filter import (
    DEFAULT_SPRINT_FILTER,
    SprintFilter,
)

log = logging.getLogger(__name__)


class FilterMixin(ProjectHomeBase):
    """Filter modal wiring for the active [1] view."""

    @on(Button.Pressed, "#hub-filter")
    def _on_filter_pressed(self) -> None:
        if self._ui.view_mode == "sprints":
            self._open_sprint_filter()
            return
        if self._ui.view_mode == "members":
            self._open_member_filter()
            return
        self.run_worker(self._open_issue_filter(), exclusive=True, group="hub-filter")

    def _open_sprint_filter(self) -> None:
        self.app.push_screen(
            SprintFilterModal(current=self._filters.sprint),
            self._on_sprint_filter_applied,
        )

    def _open_member_filter(self) -> None:
        self.app.push_screen(
            MemberFilterModal(current=self._filters.member),
            self._on_member_filter_applied,
        )

    def _on_sprint_filter_applied(self, new_filter: SprintFilter | None) -> None:
        if new_filter is None:
            return
        self._filters.sprint = new_filter
        self._update_filter_button()
        self._persist_filters()
        self.run_worker(self._load_sprints(), exclusive=True, group="hub-list")

    def _on_member_filter_applied(self, new_filter: MemberFilter | None) -> None:
        if new_filter is None:
            return
        self._filters.member = new_filter
        self._update_filter_button()
        self._persist_filters()
        self.run_worker(self._reapply_member_filter(), exclusive=True, group="hub-list")

    async def _reapply_member_filter(self) -> None:
        await self._render_members_list(self._search_keyword())
        if self._member_list.displayed:
            self._select_member(0)

    async def _open_issue_filter(self) -> None:
        await self._ensure_members_loaded()
        self.app.push_screen(
            IssueFilterModal(
                current=self._filters.issue,
                members=self._member_list.members,
            ),
            self._on_filter_applied,
        )

    def _on_filter_applied(self, new_filter: IssueFilter | None) -> None:
        if new_filter is None:
            return
        self._filters.issue = new_filter
        self._update_filter_button()
        self._persist_filters()
        self._cancel_search_timer()
        switching = self._ui.view_mode != "issues"
        if switching:
            self._set_view_chrome("issues")
        keyword = None if switching else self._search_keyword()
        self.run_worker(self._load_issues(keyword), exclusive=True, group="hub-list")
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def _update_filter_button(self) -> None:
        try:
            filter_button = self.query_one("#hub-filter", Button)
        except NoMatches:
            return
        if self._ui.view_mode == "sprints":
            active = self._filters.sprint != DEFAULT_SPRINT_FILTER
            label = "Filter sprints"
        elif self._ui.view_mode == "members":
            active = self._filters.member != DEFAULT_MEMBER_FILTER
            label = "Filter members"
        else:
            active = self._filters.issue != DEFAULT_ISSUE_FILTER
            label = "Filter issues"
        filter_button.set_class(active, "-filter-active")
        filter_button.tooltip = "Filter (active)" if active else label

    def _restore_filters(self) -> None:
        saved = self.app.config.project_filter_state(self._project_key)
        if not saved:
            return
        try:
            if "issue" in saved:
                self._filters.issue = filter_from_dict(IssueFilter, saved["issue"])
            if "member" in saved:
                self._filters.member = filter_from_dict(MemberFilter, saved["member"])
            if "sprint" in saved:
                self._filters.sprint = filter_from_dict(SprintFilter, saved["sprint"])
        except (TypeError, ValueError, AttributeError) as error:
            log.debug("Ignoring unreadable saved filters: %s", error)

    def _persist_filters(self) -> None:
        self.app.config.save_project_filters(
            self._project_key,
            {
                "issue": filter_to_dict(self._filters.issue),
                "member": filter_to_dict(self._filters.member),
                "sprint": filter_to_dict(self._filters.sprint),
            },
        )

    def _search_keyword(self) -> str | None:
        try:
            return self.query_one("#hub-search", Input).value.strip() or None
        except NoMatches:
            return None
