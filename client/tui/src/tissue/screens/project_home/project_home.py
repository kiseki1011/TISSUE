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
from tissue.screens.project_home.areas.issues import IssuesMixin
from tissue.screens.project_home.areas.layout import LayoutMixin
from tissue.screens.project_home.areas.members import MembersMixin
from tissue.screens.project_home.areas.sprints import SprintsMixin
from tissue.screens.project_home.areas.transitions import TransitionsMixin


class ProjectHomeScreen(
    IssuesMixin,
    AgentIssuesMixin,
    LayoutMixin,
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
        Binding("3", "focus_agent_issues", show=False),
        # ctrl+digit also works while the search input has focus (a plain digit is
        # typed into the input there, never reaching the screen binding).
        Binding("ctrl+1", "focus_issues", show=False),
        Binding("ctrl+2", "focus_detail", show=False),
        Binding("ctrl+3", "focus_agent_issues", show=False),
        # Cycle the [1] list through Issues / Sprints / Members (hinted in the box
        # title); a ctrl-combo so it still works while the search input has focus.
        Binding("ctrl+t", "toggle_list", show=False),
        # Collapse/restore the focused [1]/[3] box (CTRL+W).
        Binding("ctrl+w", "toggle_collapse", show=False),
        # CTRL+F expands the left column to full width, hiding [2] (and back). Shown
        # in the footer with a state-dependent label (see footer_description_overrides);
        # priority so it wins over a focused Input's own ctrl+f (word-delete).
        Binding("ctrl+f", "toggle_expand", "close details", priority=True),
        # `/` focuses search — works in every terminal (vim/less style); the only
        # search key shown in the footer. When the search input has focus a typed
        # `/` goes into it (Textual dispatches to the focused widget first), so the
        # binding only fires from the lists.
        Binding("slash", "focus_search", "search", key_display="/"),
        # Ctrl+/ does the same, kept for muscle memory — but only some terminals can
        # encode it: legacy ones send ctrl+underscore (0x1F), the kitty keyboard
        # protocol sends ctrl+slash, and IntelliJ's terminal sends neither. Hidden
        # so the footer advertises the universal `/` instead.
        Binding("ctrl+underscore,ctrl+slash", "focus_search", show=False),
        # Esc leaves the search box back to the list, so the box-jump digits (which
        # the search input would otherwise swallow) work again without a Ctrl chord.
        Binding("escape", "leave_search", show=False),
    ]

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            search = Input(placeholder="Search issues…", id="hub-search")
            search.border_title = "Search"
            # Beside the search bar: a square filter button (placeholder, no
            # handler yet) and a square "+" button that opens the create-issue
            # form — both the same compact size.
            yield Horizontal(
                search,
                Button("⚙", id="hub-filter", classes="search-filter-btn"),
                Button("+", id="hub-new-issue", classes="search-filter-btn"),
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
                issues.border_title = "[1] Issues"
                issues.border_subtitle = "CTRL+T: Sprints"
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
                # [3] box (left column, below [1]): issues assigned to agents the
                # user owns. Its own focusable host so [3] stays reachable when the
                # list is empty (mirrors the [1] list host).
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
                agent_box.border_title = "[3] Agent Work"
                yield agent_box

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        # Seed both stacked boxes' border chrome (view name + CTRL+T + Close hints).
        self._refresh_box_chrome()
        # All [1] list views share one exclusive worker group ("hub-list") so only
        # one ever renders into #hub-list-host at a time. The issues load also
        # ensures the member roster is loaded (for the Assignee column + name
        # resolution), so every _load_members runs inside this single group — no
        # separate eager load, hence no cross-group race on self._members.
        self._run_view_load("issues")
        self.run_worker(self._load_state_colors(), exclusive=True, group="hub-colors")
        # [3] Agent Work loads independently of the [1] list view.
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    async def refresh_data(self) -> None:
        # Refresh whichever list is currently shown (not always issues), via the
        # shared group so it can't race a concurrent view switch.
        self._run_view_load(self._view_mode)
        # Keep [3] Agent Work in sync too (mirrors on_mount), so `r` reloads every
        # box on the screen — not just the [1] list.
        self.run_worker(self._load_agent_issues(), exclusive=True, group="hub-agent")

    def footer_description_overrides(self) -> dict[str, str]:
        """State-dependent footer labels (applied by TissueFooter). CTRL+F closes
        the [2] details pane when it's visible, opens it when hidden."""
        return {"toggle_expand": "open details" if self._expanded else "close details"}
