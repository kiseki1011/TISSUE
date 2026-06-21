from __future__ import annotations

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid, Horizontal, Vertical, VerticalScroll
from textual.widgets import Button, Input, Static

from tissue.screens.project_home.areas.activity import ActivityMixin
from tissue.screens.project_home.areas.assign import AssignMixin
from tissue.screens.project_home.areas.comments import CommentsMixin
from tissue.screens.project_home.areas.detail import DetailMixin
from tissue.screens.project_home.areas.edits import EditsMixin
from tissue.screens.project_home.areas.issues import IssuesMixin
from tissue.screens.project_home.areas.members import MembersMixin
from tissue.screens.project_home.areas.sprints import SprintsMixin
from tissue.screens.project_home.areas.transitions import TransitionsMixin


class ProjectHomeScreen(
    IssuesMixin,
    SprintsMixin,
    MembersMixin,
    DetailMixin,
    TransitionsMixin,
    AssignMixin,
    EditsMixin,
    CommentsMixin,
    ActivityMixin,
):
    """Per-project hub: a master-detail view of the project's issues.

    The [1] box cycles between the project's Issues, Sprints and Members lists
    (CTRL+T, hinted in its border title); selecting a row renders that item's read
    view in the focusable [2] Details pane on the right (an issue's fields + inline
    edits + transition/assignee actions, a sprint's meta + issues, or a member's
    role/status).

    The screen is assembled from per-area mixins (see `areas/`); shared state and
    the cross-area method contract live in `_base.ProjectHomeBase`.
    """

    CSS_PATH = "project_home.tcss"

    # Below -h-narrow the detail's activity timeline column is hidden (CSS) so the
    # issue body keeps full width on small terminals.
    HORIZONTAL_BREAKPOINTS = [
        (0, "-h-narrow"),
        (120, "-h-wide"),
    ]

    BINDINGS = [
        Binding("1", "focus_issues", show=False),
        Binding("2", "focus_detail", show=False),
        # ctrl+digit also works while the search input has focus (a plain digit is
        # typed into the input there, never reaching the screen binding).
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
        # Cycle the [1] list through Issues / Sprints / Members (hinted in the box
        # title); a ctrl-combo so it still works while the search input has focus.
        Binding("ctrl+t", "toggle_list", show=False),
        # ctrl+/ — terminals send it as ctrl+underscore (0x1F); the kitty keyboard
        # protocol sends it as ctrl+slash. Bind both, display as ctrl+/.
        Binding(
            "ctrl+underscore,ctrl+slash",
            "focus_search",
            "search",
            key_display="ctrl+/",
        ),
    ]

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            search = Input(placeholder="Search issues…", id="hub-search")
            search.border_title = "Search"
            # A square button beside the search bar (same height); it will later
            # open a filter/sort modal. No handler yet — placeholder only.
            yield Horizontal(
                search,
                Button("⚙", id="hub-filter", classes="search-filter-btn"),
                id="hub-search-row",
            )
            with Grid(id="hub-grid"):
                # The [1] box swaps its content host between the Issues and Sprints
                # lists; ⌃T toggles (hinted in the border title — no tab chrome).
                # The host is focusable so [1] stays reachable (and focus never
                # falls to the search bar) even when the active view is empty and
                # has no table — and so a CTRL+T swap can park focus on it.
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
                issues.border_title = "[1] Issues  (CTRL+T: Sprints)"
                yield issues
                # Detail splits into the scrollable issue body (left, 3fr, the
                # focus target) and the activity timeline (right, 1fr). The body's
                # content sits in an inner wrapper that carries the padding, so the
                # scrollbar stays flush at the pane edge (outside the padding).
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

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        # All [1] list views share one exclusive worker group ("hub-list") so only
        # one ever renders into #hub-list-host at a time. The issues load also
        # ensures the member roster is loaded (for the Assignee column + name
        # resolution), so every _load_members runs inside this single group — no
        # separate eager load, hence no cross-group race on self._members.
        self._run_view_load("issues")
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")

    async def refresh_data(self) -> None:
        # Refresh whichever list is currently shown (not always issues), via the
        # shared group so it can't race a concurrent view switch.
        self._run_view_load(self._view_mode)
