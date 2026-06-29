from __future__ import annotations

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid

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
from tissue.screens.project_home.areas.relations import RelationsMixin
from tissue.screens.project_home.areas.reviewers import ReviewersMixin
from tissue.screens.project_home.areas.sprints import SprintsMixin
from tissue.screens.project_home.areas.transitions import TransitionsMixin
from tissue.screens.project_home.panels import (
    AgentWorkPanel,
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
        Binding("3", "focus_agent_issues", show=False),
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
        Binding("ctrl+3", "focus_agent_issues", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("j", "scroll_detail('down')", show=False),
        Binding("k", "scroll_detail('up')", show=False),
        Binding("e", "edit", "edit"),
        Binding("a", "assign", "assign"),
        Binding("t", "transition", "transition"),
        Binding("s", "add_to_sprint", "add to sprint"),
        Binding("ctrl+t", "toggle_list", show=False),
        Binding("ctrl+w", "toggle_collapse", show=False),
        Binding("ctrl+f", "toggle_expand", "close details", priority=True),
        Binding("slash", "focus_search", "search", key_display="/"),
        Binding("ctrl+underscore,ctrl+slash", "focus_search", show=False),
        Binding("escape", "leave_search", show=False),
    ]

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            yield ProjectSearchBar()
            with Grid(id="hub-grid"):
                yield IssueListPanel()
                yield IssueDetailPanel()
                yield AgentWorkPanel()

    def on_mount(self) -> None:
        self._restore_filters()
        self._restore_project_ui()
        self.app.config.set_last_project(self._project_key)
        self._apply_initial_breakpoints()
        self.set_class(self._ui.expanded, "-expanded")
        self._apply_collapse()
        self._refresh_box_chrome()
        self._update_filter_button()
        self._run_view_load(self._ui.view_mode, focus_list=True)
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    async def refresh_data(self) -> None:
        self._detail_state.cache.clear()
        self._run_view_load(self._ui.view_mode)
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def on_unmount(self) -> None:
        self._cancel_detail_timer()
        self._cancel_search_timer()

    def footer_description_overrides(self) -> dict[str, str]:
        return {
            "toggle_expand": "open details" if self._ui.expanded else "close details"
        }

    def action_edit(self) -> None:
        if self._ui.view_mode == "sprints":
            self._edit_sprint()
        else:
            self._edit_issue()

    def action_transition(self) -> None:
        if self._ui.view_mode == "sprints":
            self._transition_sprint()
        else:
            self._transition_issue()

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action in ("assign", "add_to_sprint"):
            return self._ui.view_mode == "issues"
        if action in ("edit", "transition"):
            if self._ui.view_mode == "issues":
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
