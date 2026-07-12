from __future__ import annotations

from textual import events
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid

from tissue.api.errors import TissueApiError
from tissue.screens.project_home.areas.activity import ActivityMixin
from tissue.screens.project_home.areas.agent_issues import AgentIssuesMixin
from tissue.screens.project_home.areas.assign import AssignMixin
from tissue.screens.project_home.areas.comments import CommentsMixin
from tissue.screens.project_home.areas.detail import DetailMixin
from tissue.screens.project_home.areas.edits import EditsMixin
from tissue.screens.project_home.areas.filtering import FilterMixin
from tissue.screens.project_home.areas.hierarchy import HierarchyMixin
from tissue.screens.project_home.areas.issues import IssuesMixin
from tissue.screens.project_home.areas.layout import LayoutMixin
from tissue.screens.project_home.areas.members import MembersMixin
from tissue.screens.project_home.areas.realtime import RealtimeMixin
from tissue.screens.project_home.areas.relations import RelationsMixin
from tissue.screens.project_home.areas.reviewers import ReviewersMixin
from tissue.screens.project_home.areas.sprints import SprintsMixin
from tissue.screens.project_home.areas.transitions import TransitionsMixin
from tissue.screens.project_home.constants import _ISSUE_VIEWS
from tissue.screens.project_home.panels import (
    ActivityPanel,
    IssueDetailPanel,
    IssueListPanel,
    ProjectSearchBar,
)


class ProjectHomeScreen(
    IssuesMixin,
    FilterMixin,
    AgentIssuesMixin,
    LayoutMixin,
    SprintsMixin,
    MembersMixin,
    DetailMixin,
    TransitionsMixin,
    AssignMixin,
    EditsMixin,
    ReviewersMixin,
    HierarchyMixin,
    RelationsMixin,
    CommentsMixin,
    ActivityMixin,
    RealtimeMixin,
):
    """Per-project hub."""

    CSS_PATH = "project_home.tcss"

    HORIZONTAL_BREAKPOINTS = [
        (0, "-h-narrow"),
        (120, "-h-wide"),
    ]

    BINDINGS = [
        Binding("1", "focus_issues", show=False),
        Binding("2", "focus_detail", show=False),
        Binding("3", "focus_activity", show=False),
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
        Binding("ctrl+3", "focus_activity", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("j", "scroll_detail('down')", show=False),
        Binding("k", "scroll_detail('up')", show=False),
        Binding("n", "create", "new"),
        Binding("e", "edit", "edit"),
        Binding("a", "assign", "assign"),
        Binding("t", "transition", "transition"),
        Binding("s", "add_to_sprint", "add to sprint"),
        Binding("x", "remove_from_sprint", "remove from sprint"),
        Binding("d", "delete", "delete"),
        Binding("v", "review", "review"),
        Binding("full_stop,greater_than_sign", "cycle_view('next')", show=False),
        Binding("comma,less_than_sign", "cycle_view('prev')", show=False),
        Binding("ctrl+w", "toggle_activity", "activity", show=False),
        Binding("ctrl+f", "toggle_expand", "close details", priority=True, show=False),
        Binding("slash", "focus_search", "search", key_display="/"),
        Binding("ctrl+underscore,ctrl+slash", "focus_search", show=False),
        Binding("escape", "leave_search", show=False),
    ]

    async def _load_header_stats(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            assigned = await client.issues.my_assigned_total(
                project_key=self._project_key,
                state_categories=["INITIAL", "ACTIVE"],
            )
            in_progress = await client.issues.my_assigned_total(
                project_key=self._project_key,
                state_categories=["ACTIVE"],
            )
        except TissueApiError:
            return
        self.set_top_bar_status(
            f"{self._project_key} · {assigned} assigned · {in_progress} in progress"
        )

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            yield ProjectSearchBar()
            with Grid(id="hub-grid"):
                yield IssueListPanel()
                yield IssueDetailPanel()
                yield ActivityPanel()

    def on_mount(self) -> None:
        self._restore_filters()
        self._restore_project_ui()
        self.app.config.set_last_project(self._project_key)
        self._apply_initial_breakpoints()
        self.set_class(self._ui.expanded, "-expanded")
        self._apply_activity_state()
        self._refresh_box_chrome()
        self._update_filter_button()
        self._run_view_load(self._ui.view_mode, focus_list=True)
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")
        self.run_worker(self._load_header_stats(), exclusive=True, group="hub-header")

    async def refresh_data(self) -> None:
        self._detail_state.cache.clear()
        self._run_view_load(self._ui.view_mode)
        self.run_worker(self._load_header_stats(), exclusive=True, group="hub-header")

    def on_unmount(self) -> None:
        self._cancel_detail_timer()
        self._cancel_search_timer()

    def footer_description_overrides(self) -> dict[str, str]:
        """`s` flips its label whether issue joines current sprint oir not."""
        return {
            "create": self._create_label(),
            "add_to_sprint": (
                "remove from sprint"
                if self._focused_in_active_sprint()
                else "add to sprint"
            ),
        }

    def _create_label(self) -> str:
        if self._ui.view_mode == "sprints":
            return "new sprint"
        if self._ui.view_mode == "members":
            return "add member"
        return "new issue"

    def action_edit(self) -> None:
        if self._ui.view_mode == "sprints":
            self._edit_sprint()
        elif self._ui.view_mode in _ISSUE_VIEWS:
            self._edit_issue()

    def action_transition(self) -> None:
        if self._ui.view_mode == "sprints":
            self._transition_sprint()
        elif self._ui.view_mode in _ISSUE_VIEWS:
            self._transition_issue()

    def on_descendant_focus(self, event: events.DescendantFocus) -> None:
        if event.widget.id == "hub-sprint-issues-table":
            self.refresh_bindings()

    def on_descendant_blur(self, event: events.DescendantBlur) -> None:
        if event.widget.id == "hub-sprint-issues-table":
            self.refresh_bindings()

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "create":
            return self._ui.view_mode in _ISSUE_VIEWS or self._is_project_manager()
        if action == "remove_from_sprint":
            focused = self.app.focused
            return focused is not None and focused.id == "hub-sprint-issues-table"
        if action in ("assign", "add_to_sprint", "delete"):
            return self._ui.view_mode in _ISSUE_VIEWS
        if action == "review":
            return (
                self._ui.view_mode == "reviews"
                and self._detail_state.issue_key is not None
            )
        if action in ("edit", "transition"):
            if self._ui.view_mode in _ISSUE_VIEWS:
                return True
            if self._ui.view_mode == "sprints":
                return self._sprint_editable()
            return False
        return super().check_action(action, parameters)

    def _sprint_editable(self) -> bool:
        return (
            self._sprint_state.detail_id is not None
            and self._is_project_manager()
            and (self._sprint_state.detail_status or "") in ("PLANNING", "ACTIVE")
        )
