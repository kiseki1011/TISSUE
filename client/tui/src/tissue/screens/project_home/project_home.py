from __future__ import annotations

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid, Horizontal, Vertical, VerticalScroll
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
    """Per-project hub, with the issue list on the left and the chosen issue on
    the right.

    Built from one mixin per area (see `areas/`). Shared state and the methods
    each area shares live in `_base.ProjectHomeBase`.
    """

    CSS_PATH = "project_home.tcss"

    # On a narrow terminal the detail's activity timeline column is hidden by CSS
    # so the issue body keeps the full width.
    HORIZONTAL_BREAKPOINTS = [
        (0, "-h-narrow"),
        (120, "-h-wide"),
    ]

    BINDINGS = [
        Binding("1", "focus_issues", show=False),
        Binding("2", "focus_detail", show=False),
        Binding("3", "focus_agent_issues", show=False),
        # ctrl+digit also reaches us while the search input has focus, where a
        # plain digit would just be typed into the box instead.
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
        Binding("ctrl+3", "focus_agent_issues", show=False),
        # vim-style, only on this data screen. j/k on a table moves its row
        # cursor (its own binding), on the [2] detail it scrolls the body.
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("j", "scroll_detail('down')", show=False),
        Binding("k", "scroll_detail('up')", show=False),
        # Adds focused [1] Issues issue to project's active sprint.
        Binding("ctrl+s", "add_to_sprint", show=False),
        # A ctrl-combo so it still works while the search input has focus.
        Binding("ctrl+t", "toggle_list", show=False),
        Binding("ctrl+w", "toggle_collapse", show=False),
        # priority so it beats a focused Input's own ctrl+f (word-delete). The
        # footer label changes with the state (see footer_description_overrides).
        Binding("ctrl+f", "toggle_expand", "close details", priority=True),
        # When the search input has focus a typed `/` goes into it (Textual sends
        # the key to the focused widget first), so this only fires from the lists.
        Binding("slash", "focus_search", "search", key_display="/"),
        # Same as `/`, kept for muscle memory. Terminals disagree on the encoding.
        # - Legacy terminals send `ctrl+_` (0x1F)
        # - Kitty sends `ctrl+/`
        # - IntelliJ sends neither
        Binding("ctrl+underscore,ctrl+slash", "focus_search", show=False),
        # Brings back the box-jump digits, which the search input would otherwise eat.
        Binding("escape", "leave_search", show=False),
    ]

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            search = Input(placeholder="Search issues…", id="hub-search")
            search.border_title = "Search"
            filter_btn = Button("⚙", id="hub-filter", classes="search-filter-btn")
            filter_btn.tooltip = "Filter issues"
            yield Horizontal(
                search,
                filter_btn,
                Button("+", id="hub-new-issue", classes="search-filter-btn"),
                id="hub-search-row",
            )
            with Grid(id="hub-grid"):
                # The host can take focus so [1] stays reachable (and focus never
                # falls to the search bar) even when the open view is empty and has
                # no table, and so a CTRL+T swap can rest focus on it.
                list_host = Vertical(
                    Static("Loading…", classes="hub-muted"),
                    id="hub-list-host",
                    classes="hub-list-host",
                )
                list_host.can_focus = True
                issues = Vertical(
                    list_host,
                    id="hub-issues-box",
                    classes="hub-box panel",
                )
                issues.border_title = "[1] Issues"
                issues.border_subtitle = "CTRL+T: Sprints"
                yield issues
                # Body content sits in an inner wrapper that holds the padding, so
                # the scrollbar stays at the pane edge, outside the padding.
                main = VerticalScroll(
                    Vertical(
                        Static("Select an issue to see details.", classes="hub-muted"),
                        id="hub-detail-main-inner",
                    ),
                    id="hub-detail-main",
                )
                main.can_focus = True
                detail = Horizontal(
                    main,
                    VerticalScroll(
                        Vertical(id="hub-detail-timeline-inner"),
                        id="hub-detail-timeline",
                    ),
                    id="hub-detail",
                )
                detail.border_title = "[2] Details"
                yield detail
                # Its own host that can take focus, so [3] stays reachable when the
                # list is empty (same idea as the [1] list host).
                agent_host = Vertical(
                    Static("Loading…", classes="hub-muted"),
                    id="hub-agent-issues-host",
                    classes="hub-list-host",
                )
                agent_host.can_focus = True
                agent_box = Vertical(
                    agent_host,
                    id="hub-agent-issues-box",
                    classes="hub-box panel",
                )
                agent_box.border_title = "[3] My Agent's Work"
                yield agent_box

    def on_mount(self) -> None:
        # Restore this project's saved filters before the first load reads them,
        # and remember we're here so the next launch reopens this hub.
        self._restore_filters()
        self.app.config.set_last_project(self._project_key)
        self._apply_initial_breakpoints()
        self._refresh_box_chrome()
        # Reflect restored, non-default filters on the ⚙ button.
        self._update_filter_button()
        # All [1] list views share one worker group that lets only one run, so only
        # one draws into #hub-list-host at a time. Loading the issues also makes
        # sure the member list is loaded, so every _load_members runs in this one
        # group, with no separate early load, so nothing else can change
        # self._members at the same time. focus_list opens the screen on the issues
        # table once it mounts, not the search box.
        self._run_view_load("issues", focus_list=True)
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    async def refresh_data(self) -> None:
        """Reload whichever list is shown (not always issues), through the shared group
        so it can't collide with a view switch happening at the same time.

        [3] mirrors on_mount so `r` reloads every box, not just the [1] list.
        """

        # Drop the detail cache on refresh
        self._detail_cache.clear()
        self._run_view_load(self._view_mode)
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def on_unmount(self) -> None:
        """Don't let a waiting timer fire on a screen that's going away."""
        self._cancel_detail_timer()
        self._cancel_search_timer()

    def footer_description_overrides(self) -> dict[str, str]:
        """Footer labels that change with the state, applied by TissueFooter."""
        return {"toggle_expand": "open details" if self._expanded else "close details"}
