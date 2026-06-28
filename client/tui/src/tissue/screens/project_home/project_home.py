from __future__ import annotations

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid, Horizontal, Vertical, VerticalScroll
from textual.widget import Widget
from textual.widgets import Button, Input, Static

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
        Binding("ctrl+s", "add_to_sprint", show=False),
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
            yield self._search_row()
            with Grid(id="hub-grid"):
                yield self._issues_box()
                yield self._detail_box()
                yield self._agent_box()

    def _search_row(self) -> Widget:
        search = Input(placeholder="Search issues…", id="hub-search")
        search.border_title = "Search"
        filter_btn = Button("⚙", id="hub-filter", classes="search-filter-btn")
        filter_btn.tooltip = "Filter issues"
        return Horizontal(
            search,
            filter_btn,
            Button("+", id="hub-new-issue", classes="search-filter-btn"),
            id="hub-search-row",
        )

    def _issues_box(self) -> Widget:
        list_host = self._focusable_list_host("hub-list-host")
        box = Vertical(list_host, id="hub-issues-box", classes="hub-box panel")
        box.border_title = "[1] Issues"
        box.border_subtitle = "CTRL+T: Sprints"
        return box

    def _detail_box(self) -> Widget:
        main = VerticalScroll(
            Vertical(
                Static("Select an issue to see details.", classes="hub-muted"),
                id="hub-detail-main-inner",
            ),
            id="hub-detail-main",
        )
        main.can_focus = True
        box = Horizontal(
            main,
            VerticalScroll(
                Vertical(id="hub-detail-timeline-inner"),
                id="hub-detail-timeline",
            ),
            id="hub-detail",
        )
        box.border_title = "[2] Details"
        return box

    def _agent_box(self) -> Widget:
        agent_host = self._focusable_list_host("hub-agent-issues-host")
        box = Vertical(agent_host, id="hub-agent-issues-box", classes="hub-box panel")
        box.border_title = "[3] My Agent's Work"
        return box

    def _focusable_list_host(self, widget_id: str) -> Vertical:
        host = Vertical(
            Static("Loading…", classes="hub-muted"),
            id=widget_id,
            classes="hub-list-host",
        )
        host.can_focus = True
        return host

    def on_mount(self) -> None:
        self._restore_filters()
        self._restore_project_ui()
        self.app.config.set_last_project(self._project_key)
        self._apply_initial_breakpoints()
        self.set_class(self._expanded, "-expanded")
        self._apply_collapse()
        self._refresh_box_chrome()
        self._update_filter_button()
        self._run_view_load(self._view_mode, focus_list=True)
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    async def refresh_data(self) -> None:
        self._detail_cache.clear()
        self._run_view_load(self._view_mode)
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def on_unmount(self) -> None:
        self._cancel_detail_timer()
        self._cancel_search_timer()

    def footer_description_overrides(self) -> dict[str, str]:
        return {"toggle_expand": "open details" if self._expanded else "close details"}
