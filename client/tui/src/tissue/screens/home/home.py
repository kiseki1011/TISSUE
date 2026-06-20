from __future__ import annotations

import logging
from typing import cast

from rich.markdown import Markdown as RichMarkdown
from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Grid, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.timer import Timer
from textual.widget import Widget
from textual.widgets import DataTable, Input, Label, Rule, Static
from textual_autocomplete import AutoComplete, DropdownItem, TargetState

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.issue_summary import IssueSummary
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_date, format_relative
from tissue.widgets.detail_row import detail_row

log = logging.getLogger(__name__)

_PREVIEW_COUNT = 5
_SEARCH_SIZE = 20

# Live search (the [1] Searched Items box): the keyword (the part after the
# /project: /wiki: /issue: prefix) must reach this many characters before we hit
# the API, and we wait this long after the last keystroke (debounce) before
# firing — mirrors the wiki reader's live search.
_MIN_QUERY_LEN = 2
_SEARCH_DEBOUNCE = 0.2

# Key column widths (chars)
# Keys longer than this are clipped with a trailing "…"
_PROJECT_KEY_WIDTH = 11
_ISSUE_KEY_WIDTH = 14

# Search-bar command prefixes
_SEARCH_PREFIXES = {"/project:": "project", "/wiki:": "wiki", "/issue:": "issue"}


class _DashTable(DataTable):
    """Table that self-populates on mount."""

    BINDINGS = [
        Binding("j", "cursor_down", show=False),
        Binding("k", "cursor_up", show=False),
    ]

    def __init__(
        self,
        columns: list[tuple[str, int | None]],
        rows: list[list[str | Text]],
        *,
        id: str | None = None,
        classes: str | None = None,
    ) -> None:
        super().__init__(
            cursor_type="row",
            zebra_stripes=True,
            cell_padding=2,
            id=id,
            classes=classes,
        )
        self._dash_columns = columns
        self._dash_rows = rows

    def on_mount(self) -> None:
        for label, width in self._dash_columns:
            if width is None:
                self.add_column(label)
            else:
                self.add_column(label, width=width)
        for row in self._dash_rows:
            self.add_row(*row)
        self.show_cursor = bool(self._dash_rows)


class HomeScreen(RefreshableScreen):
    """Dashboard landing screen.

    Top is a search bar (/project: /wiki: /issue:)
    [1] Searched Items  | Details
    [2] Projects        |
    [3] Recent Wiki     | [4] My Work
    """

    CSS_PATH = "home.tcss"

    # Number keys jump to a box.
    # h/l cycle through the boxes (1→2→3→4→1).
    # j/k (and the arrows) move rows inside the focused table.
    # c / p create a project / toggle pin while the [2] Projects box is focused.
    BINDINGS = [
        Binding("1", "focus_box('dash-searched')", show=False),
        Binding("2", "focus_box('dash-projects-box')", show=False),
        Binding("3", "focus_box('dash-wiki-box')", show=False),
        Binding("4", "focus_box('dash-mywork')", show=False),
        # ctrl+digit does the same jump but also works while the search input has
        # focus (a plain digit is typed into the input there, never reaching the
        # screen binding — see Textual's focused-widget-first key dispatch).
        Binding("ctrl+1", "focus_box('dash-searched')", show=False),
        Binding("ctrl+2", "focus_box('dash-projects-box')", show=False),
        Binding("ctrl+3", "focus_box('dash-wiki-box')", show=False),
        Binding("ctrl+4", "focus_box('dash-mywork')", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("c", "create_project", "create project"),
        Binding("p", "toggle_pin", "pin/unpin"),
        # ctrl+/ — terminals send it as ctrl+underscore (0x1F); the kitty keyboard
        # protocol sends it as ctrl+slash. Bind both, display as ctrl+/.
        Binding(
            "ctrl+underscore,ctrl+slash",
            "focus_search",
            "search",
            key_display="ctrl+/",
        ),
    ]

    _BOX_IDS = (
        "dash-searched",
        "dash-projects-box",
        "dash-wiki-box",
        "dash-mywork",
    )

    def __init__(self) -> None:
        super().__init__()
        self._projects: list[ProjectSummary] | None = None
        self._recent_wiki: list[WikiDocumentSearchResult] | None = None
        self._my_work: list[IssueSummary] | None = None
        self._search_type: str | None = None
        self._search_results: (
            list[ProjectSummary]
            | list[WikiDocumentSearchResult]
            | list[IssueSummary]
            | None
        ) = None
        # The keyword behind the current results, for title highlighting.
        self._search_keyword = ""
        # Bumped on every search/reset/refresh so a slow in-flight search whose
        # result lands late can't clobber a newer search or a reset.
        self._search_gen = 0
        # Pending debounce timer for live search (restarted on each keystroke).
        self._search_timer: Timer | None = None
        # The search kind ("project"/"wiki"/"issue") whose table is currently
        # mounted in the Searched Items box, or None when a placeholder Static is
        # shown. Lets _render_searched refill rows in place when only the rows
        # changed (same kind => same columns) instead of remounting the whole
        # table, which would make the column header flicker on every keystroke.
        self._searched_table_kind: str | None = None

    def top_bar_breadcrumb(self) -> str:
        return "Dashboard"

    def compose_content(self) -> ComposeResult:
        with Vertical(id="screen-body"):
            search = Input(
                placeholder="/project:<kw>   /wiki:<kw>   /issue:<kw>",
                id="dashboard-search",
            )
            search.border_title = "Search"
            yield search
            yield AutoComplete(search, candidates=self._search_candidates)
            with Grid(id="dashboard-grid"):
                yield self._box(
                    "[1] Searched Items", "dash-searched", self._searched_widgets()
                )
                yield self._detail_box()
                yield self._box(
                    "[2] Projects", "dash-projects-box", self._projects_widgets()
                )
                yield self._box(
                    "[3] Recent Wiki", "dash-wiki-box", self._wiki_widgets()
                )
                yield self._box("[4] My Work", "dash-mywork", self._mywork_widgets())

    # ---- search bar ----------------------------------------------------

    def _search_candidates(self, state: TargetState) -> list[DropdownItem]:
        """Suggest the command prefixes only while typing the prefix."""
        if ":" in state.text:
            return []
        return [DropdownItem(prefix) for prefix in _SEARCH_PREFIXES]

    @staticmethod
    def _parse_search(raw: str) -> tuple[str, str] | None:
        raw = raw.strip()
        for prefix, kind in _SEARCH_PREFIXES.items():
            if raw.startswith(prefix):
                return kind, raw[len(prefix) :].strip()
        return None

    def _cancel_search_timer(self) -> None:
        if self._search_timer is not None:
            self._search_timer.stop()
            self._search_timer = None

    def on_unmount(self) -> None:
        # Don't let a pending debounce fire on a screen that's going away.
        self._cancel_search_timer()

    @on(Input.Changed, "#dashboard-search")
    def _on_search_changed(self, event: Input.Changed) -> None:
        # Live search: (re)start the debounce timer so the search fires only once
        # typing pauses. We need a /project: /wiki: /issue: prefix and a keyword
        # of at least _MIN_QUERY_LEN chars; anything shorter clears any results
        # back to the prompt. Focus stays in the input so the user keeps typing.
        self._cancel_search_timer()
        parsed = self._parse_search(event.value)
        if parsed is None or len(parsed[1]) < _MIN_QUERY_LEN:
            self._reset_search()
            return
        kind, keyword = parsed
        self._search_timer = self.set_timer(
            _SEARCH_DEBOUNCE,
            lambda: self.run_worker(
                self._run_search(kind, keyword),
                exclusive=True,
                group="dash-search",
            ),
        )

    @on(Input.Submitted, "#dashboard-search")
    def _on_search_submitted(self, event: Input.Submitted) -> None:
        # Enter searches immediately (skipping the debounce). Unlike live search
        # it allows an empty keyword (browse everything for that kind).
        self._cancel_search_timer()
        parsed = self._parse_search(event.value)
        if parsed is None:
            self.app.notify(
                "Use /project:, /wiki: or /issue: followed by a keyword.",
                severity="warning",
            )
            return
        kind, keyword = parsed
        self.run_worker(
            self._run_search(kind, keyword), exclusive=True, group="dash-search"
        )

    def _reset_search(self) -> None:
        """Clear the Searched Items box back to its prompt (e.g. the keyword fell
        below the minimum)."""
        # Bump the generation FIRST, unconditionally. An in-flight search may not
        # have assigned its results yet — so the guard below would see None/None
        # and skip — but it must still be invalidated, or it would render results
        # for a query the user has already backspaced away (a desync against the
        # now-too-short input). _run_search re-checks the gen after its await and
        # drops the stale result.
        self._search_gen += 1
        if self._search_type is None and self._search_results is None:
            # Nothing on screen to clear (the bump above already cancelled any
            # in-flight search) — skip the re-render so typing the prefix doesn't
            # rebuild the box on every keystroke.
            return
        self._search_results = None
        self._search_type = None
        self._search_keyword = ""
        self.run_worker(self._render_searched(), exclusive=True, group="dash-search")

    async def _run_search(self, kind: str, keyword: str) -> None:
        self._search_gen += 1
        gen = self._search_gen
        client = self.app.client
        if client is None:
            return
        results: (
            list[ProjectSummary] | list[WikiDocumentSearchResult] | list[IssueSummary]
        )
        try:
            if kind == "project":
                page = await client.projects.list_projects(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                results = list(page.content or [])
            elif kind == "wiki":
                wiki_page = await client.wiki.search(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                results = list(wiki_page.content or [])
            else:  # issue
                issue_page = await client.issues.search(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                results = list(issue_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard search (%s) failed: %s", kind, e)
            self.app.notify("Search failed. Please try again.", severity="error")
            return
        if gen != self._search_gen:  # superseded by a newer search or a reset
            return
        self._search_results = results
        self._search_type = kind
        self._search_keyword = keyword
        await self._render_searched()

    def _searched_table_data(
        self,
    ) -> tuple[list[tuple[str, int | None]], list[list[str | Text]]] | None:
        """The (columns, rows) for the current search results, or None when a
        placeholder Static should be shown instead (not yet searched / no
        results). Columns depend only on the search kind, so two calls with the
        same `_search_type` always return the same column spec."""
        if not self._search_results:  # None (not searched) or empty (no matches)
            return None
        if self._search_type == "project":
            projects = cast("list[ProjectSummary]", self._search_results)
            columns: list[tuple[str, int | None]] = [
                ("Key", _PROJECT_KEY_WIDTH),
                ("Title", None),
                ("Visibility", 10),
                ("Created", 10),
            ]
            rows: list[list[str | Text]] = [
                [
                    self._fit(p.key or "-", _PROJECT_KEY_WIDTH),
                    self._highlight_title(p.title or "-"),
                    self._visibility_label(p.visibility),
                    format_date(p.created_at),
                ]
                for p in projects
            ]
            return columns, rows
        if self._search_type == "wiki":
            wikis = cast("list[WikiDocumentSearchResult]", self._search_results)
            columns = [("Title", None), ("Updated", 10), ("Created", 10)]
            rows = [
                [
                    self._highlight_title(d.title or "-"),
                    format_date(d.last_modified_at),
                    format_date(d.created_at),
                ]
                for d in wikis
            ]
            return columns, rows
        # issue
        issues = cast("list[IssueSummary]", self._search_results)
        columns = [
            ("Key", _ISSUE_KEY_WIDTH),
            ("Title", None),
            ("Status", 12),
            ("Pri", 4),
        ]
        rows = [
            [
                self._fit(i.issue_key or "-", _ISSUE_KEY_WIDTH),
                self._highlight_title(i.title or "-"),
                i.current_state_label or "-",
                i.priority or "-",
            ]
            for i in issues
        ]
        return columns, rows

    def _searched_widgets(self) -> list[Widget]:
        if self._search_results is None:
            return [Static("Search above (ctrl+/)", classes="dashboard-muted")]
        data = self._searched_table_data()
        if data is None:  # searched, but no matches
            return [Static("No results.", classes="dashboard-muted")]
        columns, rows = data
        return [
            _DashTable(
                columns, rows, id="dash-searched-table", classes="dashboard-table"
            )
        ]

    @staticmethod
    def _refill_table(table: DataTable, rows: list[list[str | Text]]) -> None:
        """Replace only the rows of an already-mounted table, keeping its columns
        (and thus the column header) so a live-search refresh doesn't flicker.
        `clear()` drops the rows but not the columns and resets the cursor."""
        table.clear()
        for row in rows:
            table.add_row(*row)
        table.show_cursor = bool(rows)

    async def _render_searched(self) -> None:
        try:
            box = self.query_one("#dash-searched")
        except NoMatches:
            return
        data = self._searched_table_data()
        # Fast path: the search kind is unchanged and a table is already mounted,
        # so only the rows differ. Refill them in place — remounting the table
        # would redraw (flicker) the column header on every keystroke.
        if data is not None and self._search_type == self._searched_table_kind:
            try:
                table = box.query_one("#dash-searched-table", DataTable)
            except NoMatches:
                table = None
            if table is not None:
                self._refill_table(table, data[1])
                return
        # Otherwise (placeholder <-> table, or the column schema changed): a full
        # swap. The table has a fixed id, so the old one must be gone before the
        # new one mounts (else DuplicateIds).
        await box.remove_children()
        await box.mount(*self._searched_widgets())
        self._searched_table_kind = self._search_type if data is not None else None

    # ---- box builders --------------------------------------------------

    def _box(self, title: str, box_id: str, children: list[Widget]) -> Vertical:
        box = Vertical(*children, id=box_id, classes="dashboard-box panel")
        box.border_title = title
        return box

    def _detail_box(self) -> VerticalScroll:
        inner = Vertical(
            Static("Select an item to see details.", classes="dashboard-muted"),
            id="dashboard-detail-inner",
        )
        box = VerticalScroll(inner, id="dashboard-detail", classes="dashboard-box")
        box.border_title = "Details"
        box.can_focus = False  # not a focus/nav target
        return box

    def _mywork_widgets(self) -> list[Widget]:
        if self._my_work is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._my_work:
            return [Static("Nothing assigned to you.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            [
                self._fit(i.issue_key or "-", _ISSUE_KEY_WIDTH),
                Text(self._truncate(i.title or "-")),
                i.current_state_label or "-",
                i.priority or "-",
            ]
            for i in self._my_work
        ]
        return [
            _DashTable(
                [
                    ("Key", _ISSUE_KEY_WIDTH),
                    ("Title", None),
                    ("Status", 12),
                    ("Pri", 4),
                ],
                rows,
                id="dash-mywork-table",
                classes="dashboard-table",
            )
        ]

    def _projects_widgets(self) -> list[Widget]:
        if self._projects is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._projects:
            return [
                Static(
                    "No projects yet — press c to create.",
                    classes="dashboard-muted",
                )
            ]
        rows: list[list[str | Text]] = []
        for p in self._projects:
            marker = "📌 " if self._is_pinned(p.key) else ""
            cells = [
                self._fit(p.key or "-", _PROJECT_KEY_WIDTH),
                marker + self._truncate(p.title or "-"),
                self._visibility_label(p.visibility),
                format_date(p.created_at),
            ]
            if p.archived:
                rows.append([Text(c, style="dim") for c in cells])
            else:
                rows.append([cells[0], Text(cells[1]), cells[2], cells[3]])
        return [
            _DashTable(
                [
                    ("Key", _PROJECT_KEY_WIDTH),
                    ("Title", None),
                    ("Visibility", 10),
                    ("Created", 10),
                ],
                rows,
                id="dash-projects",
                classes="dashboard-table",
            )
        ]

    def _wiki_widgets(self) -> list[Widget]:
        if self._recent_wiki is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._recent_wiki:
            return [Static("No documents yet.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            [
                Text(self._truncate(d.title or "-")),
                format_date(d.last_modified_at),
                format_date(d.created_at),
            ]
            for d in self._recent_wiki[:_PREVIEW_COUNT]
        ]
        # TODO(Phase 1): add a Tags column once wiki tags are exposed.
        return [
            _DashTable(
                [("Title", None), ("Updated", 10), ("Created", 10)],
                rows,
                id="dash-wiki",
                classes="dashboard-table",
            )
        ]

    # ---- data load -----------------------------------------------------

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_dashboard(), exclusive=True, group="dashboard-load")

    async def refresh_data(self) -> None:
        self._cancel_search_timer()  # drop any pending live-search keystroke
        self._search_gen += 1  # invalidate any in-flight search
        # A full refresh recomposes (clearing the search input), so clear the
        # search results too — otherwise the Searched Items box would keep stale
        # (highlighted) results while the input reads empty.
        self._search_results = None
        self._search_type = None
        self._search_keyword = ""
        self._searched_table_kind = None  # recompose drops the mounted table
        await self._load_dashboard()

    async def _load_dashboard(self) -> None:
        client = self.app.client
        if client is None:
            return
        # [2] Projects
        await self._fetch_projects()
        # [3] Recent Wiki
        try:
            wiki_page = await client.wiki.search(keyword=None, size=_PREVIEW_COUNT)
            self._recent_wiki = list(wiki_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard: failed to load recent wiki: %s", e)
            self._recent_wiki = []
        # [4] My Work
        try:
            mywork_page = await client.issues.my_work(size=_SEARCH_SIZE)
            self._my_work = list(mywork_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard: failed to load my work: %s", e)
            self._my_work = []
        self.refresh(recompose=True)
        # Land on a data box so the nav keys (1-4/h/l/j/k) work right away
        self.call_after_refresh(self._focus_after_load)

    def _focus_after_load(self) -> None:
        focused = self.focused
        if focused is None or focused.id == "dashboard-search":
            self._focus_box("dash-projects-box")

    # ---- project pinning (client-side) --------------------------------

    async def _fetch_projects(self) -> None:
        """Load [2] Projects (including archived) into `_projects`, pinned first."""
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.projects.list_projects(size=100, include_archived=True)
            self._projects = list(page.content or [])
            self._sort_projects()
        except TissueApiError as e:
            log.debug("Dashboard: failed to load projects: %s", e)
            self._projects = []

    async def _render_projects(self, *, focus_key: str | None = None) -> None:
        """Re-render only the [2] Projects box (after a pin toggle or create).

        Rebuilds the table from scratch, so its cursor resets to row 0 and the
        freshly-mounted table posts RowHighlighted before focus lands (the
        has_focus-gated preview misses it). After mounting we restore the cursor
        to `focus_key` — whose index shifts when pinned projects float up — and
        drive the detail preview explicitly so it stays on the acted-on project.
        """
        try:
            box = self.query_one("#dash-projects-box")
        except NoMatches:
            return
        # Await the removal: the table has a fixed id, so mounting a new one
        # before the old is gone would raise DuplicateIds.
        await box.remove_children()
        await box.mount(*self._projects_widgets())
        self.call_after_refresh(self._after_projects_render, focus_key)

    def _after_projects_render(self, focus_key: str | None) -> None:
        self._focus_box("dash-projects-box")
        try:
            table = self.query_one("#dash-projects", DataTable)
        except NoMatches:
            return
        if not self._projects or not table.row_count:
            return
        row = 0
        if focus_key is not None:
            row = next(
                (i for i, p in enumerate(self._projects) if p.key == focus_key), 0
            )
            table.move_cursor(row=row, animate=False)
        self._select_project(row)

    def _pinned_keys(self) -> set[str]:
        server = self.app.config.state.current_server_url or ""
        return set(self.app.config.pinned_project_keys(server))

    def _is_pinned(self, key: str | None) -> bool:
        return bool(key) and key in self._pinned_keys()

    def _sort_projects(self) -> None:
        """Float pinned projects to the top, preserving server order otherwise."""
        if not self._projects:
            return
        pinned = self._pinned_keys()
        self._projects.sort(key=lambda p: (p.key or "") not in pinned)

    # ---- selection → detail pane --------------------------------------

    @on(DataTable.RowHighlighted, "#dash-projects")
    def _on_project_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_project(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-projects")
    def _on_project_selected(self, event: DataTable.RowSelected) -> None:
        self._open_project(event.cursor_row)

    @on(DataTable.RowHighlighted, "#dash-wiki")
    def _on_wiki_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_wiki(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-wiki")
    def _on_wiki_selected(self, event: DataTable.RowSelected) -> None:
        self._select_wiki(event.cursor_row)

    @on(DataTable.RowHighlighted, "#dash-searched-table")
    def _on_searched_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_searched(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-searched-table")
    def _on_searched_selected(self, event: DataTable.RowSelected) -> None:
        self._select_searched(event.cursor_row)

    @on(DataTable.RowHighlighted, "#dash-mywork-table")
    def _on_mywork_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_mywork(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-mywork-table")
    def _on_mywork_selected(self, event: DataTable.RowSelected) -> None:
        self._select_mywork(event.cursor_row)

    def _select_project(self, idx: int) -> None:
        if self._projects and 0 <= idx < len(self._projects):
            self._render_project_detail(self._projects[idx], show_open_hint=True)

    def _open_project(self, idx: int) -> None:
        if not self._projects or not (0 <= idx < len(self._projects)):
            return
        project = self._projects[idx]
        if not project.key:
            return
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        self.app.push_screen(ProjectHomeScreen(project.key, title=project.title))

    def _select_wiki(self, idx: int) -> None:
        if self._recent_wiki and 0 <= idx < len(self._recent_wiki):
            self.run_worker(
                self._render_wiki_detail(self._recent_wiki[idx]),
                exclusive=True,
                group="wiki-detail",
            )

    def _select_mywork(self, idx: int) -> None:
        if self._my_work and 0 <= idx < len(self._my_work):
            self._render_issue_detail(self._my_work[idx])

    def _select_searched(self, idx: int) -> None:
        results = self._search_results
        if not results or not (0 <= idx < len(results)):
            return
        item = results[idx]
        if self._search_type == "project":
            self._render_project_detail(cast("ProjectSummary", item))
        elif self._search_type == "wiki":
            self.run_worker(
                self._render_wiki_detail(cast("WikiDocumentSearchResult", item)),
                exclusive=True,
                group="wiki-detail",
            )
        elif self._search_type == "issue":
            self._render_issue_detail(cast("IssueSummary", item))

    # ---- box focus navigation (number keys + h/l) ---------------------

    def action_focus_search(self) -> None:
        try:
            self.query_one("#dashboard-search", Input).focus()
        except NoMatches:
            pass

    def action_focus_box(self, box_id: str) -> None:
        self._focus_box(box_id)

    def action_nav(self, direction: str) -> None:
        """Cycle focus through the boxes.

        l: next
        h: previous
        """
        order = self._BOX_IDS
        current = self._current_box_id()
        if current not in order:  # nothing focused yet
            self._focus_box(order[0] if direction == "l" else order[-1])
            return
        step = 1 if direction == "l" else -1
        self._focus_box(order[(order.index(current) + step) % len(order)])

    def _focus_box(self, box_id: str) -> None:
        try:
            box = self.query_one(f"#{box_id}")
        except NoMatches:
            return
        table = next(iter(box.query(DataTable)), None)
        if table is not None:
            box.can_focus = False  # not a focus stop while it holds a table
            table.focus()
        else:  # focus the container for the highlight
            box.can_focus = True
            box.focus()

    def _current_box_id(self) -> str | None:
        """Which box contains the currently focused widget."""
        node = self.focused
        while node is not None:
            if node.id in self._BOX_IDS:
                return node.id
            node = node.parent
        return None

    # ---- project actions (create / pin), [2] Projects box only ---------

    def _projects_box_focused(self) -> bool:
        return self._current_box_id() == "dash-projects-box"

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        """Only show `c` / `p`   in the footer, when focused on [2] Projects"""

        if action in ("create_project", "toggle_pin"):
            return self._projects_box_focused()
        return True

    def action_create_project(self) -> None:
        if not self._projects_box_focused():
            return
        from tissue.screens.home.create_project_modal import CreateProjectModal

        self.app.push_screen(CreateProjectModal(), self._on_project_created)

    def _on_project_created(self, created_key: str | None) -> None:
        if created_key:
            self.run_worker(
                self._reload_projects(), exclusive=True, group="dashboard-projects"
            )

    async def _reload_projects(self) -> None:
        await self._fetch_projects()
        await self._render_projects()

    def action_toggle_pin(self) -> None:
        if not self._projects_box_focused() or not self._projects:
            return
        try:
            table = self.query_one("#dash-projects", DataTable)
        except NoMatches:
            return
        idx = table.cursor_row
        if not (0 <= idx < len(self._projects)):
            return
        key = self._projects[idx].key
        if not key:
            return
        server = self.app.config.state.current_server_url or ""
        self.app.config.toggle_pinned_project(server, key)
        self._sort_projects()
        self.run_worker(
            self._render_projects(focus_key=key),
            exclusive=True,
            group="dashboard-projects",
        )

    # ---- detail rendering ---------------------------------------------

    @staticmethod
    def _truncate(text: str, limit: int = 25) -> str:
        return text if len(text) <= limit else text[:limit] + "…"

    @staticmethod
    def _fit(text: str, width: int) -> str:
        """Clip to a fixed column width, marking overflow with a trailing ellipsis."""
        return text if len(text) <= width else text[: width - 1] + "…"

    def _highlight_title(self, title: str) -> Text:
        """A title cell (truncated) with each case-insensitive occurrence of the
        current search keyword highlighted (bold on the primary colour, matching
        the wiki reader). No active keyword → plain text, no highlight."""
        truncated = self._truncate(title)
        keyword = self._search_keyword
        text = Text()
        if not keyword:
            text.append(truncated)
            return text
        primary = self.app.theme_variables.get("primary")
        kw_style = f"bold on {primary}" if primary else "bold reverse"
        low = truncated.casefold()
        kl = keyword.casefold()
        i = 0
        while True:
            j = low.find(kl, i)
            if j == -1:
                text.append(truncated[i:])
                return text
            if j > i:
                text.append(truncated[i:j])
            text.append(truncated[j : j + len(keyword)], style=kw_style)
            i = j + len(keyword)

    @staticmethod
    def _key_detail_row(value: str) -> Horizontal:
        return Horizontal(
            Label("Key:", classes="detail-key"),
            Label(Text(value), classes="detail-value dashboard-key-value"),
            classes="detail-row",
        )

    def _render_project_detail(
        self, p: ProjectSummary, *, show_open_hint: bool = False
    ) -> None:
        widgets: list[Widget] = [
            Static(p.title or "-", markup=False, classes="dashboard-detail-title"),
            self._key_detail_row(p.key or "-"),
            detail_row("Visibility", self._visibility_label(p.visibility)),
            detail_row("Created", format_relative(p.created_at)),
            detail_row("Updated", format_relative(p.last_updated_at)),
            detail_row("Archived", "Yes" if p.archived else "No"),
            Static(
                p.description or "No description.",
                markup=False,
                classes="dashboard-detail-desc",
            ),
        ]

        if show_open_hint:
            widgets.append(
                Static("Press Enter to open", classes="dashboard-detail-hint")
            )
        self._mount_detail(widgets)

    def _render_issue_detail(self, i: IssueSummary) -> None:
        story = "-" if i.story_point is None else str(i.story_point)
        self._mount_detail(
            [
                Static(i.title or "-", markup=False, classes="dashboard-detail-title"),
                self._key_detail_row(i.issue_key or "-"),
                detail_row("Status", i.current_state_label or "-"),
                detail_row("Category", i.current_state_category or "-"),
                detail_row("Priority", i.priority or "-"),
                detail_row("Story pts", story),
                detail_row("Due", format_relative(i.due_at)),
            ]
        )

    def _wiki_meta(self, d: WikiDocumentSearchResult) -> list[Widget]:
        return [
            Static(d.title or "-", markup=False, classes="dashboard-detail-title"),
            detail_row("Version", d.current_version or "-"),
            detail_row("Locked", "🔒" if d.locked else "-"),
            detail_row("Modified", format_relative(d.last_modified_at)),
            detail_row("Created", format_relative(d.created_at)),
        ]

    async def _render_wiki_detail(self, d: WikiDocumentSearchResult) -> None:
        client = self.app.client
        if client is None or d.id is None:
            self._mount_detail(self._wiki_meta(d))
            return
        try:
            doc = await client.wiki.get_document(d.id)
        except TissueApiError as e:
            log.debug("Dashboard: failed to load wiki content: %s", e)
            self._mount_detail(
                [
                    *self._wiki_meta(d),
                    Static("Couldn't load content.", classes="dashboard-muted"),
                ]
            )
            return
        content = (doc.content or "").strip()
        body: Widget = (
            Static(RichMarkdown(content), classes="dashboard-markdown")
            if content
            else Static("No content.", classes="dashboard-muted")
        )
        self._mount_detail([*self._wiki_meta(d), Rule(), body])

    def _mount_detail(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#dashboard-detail-inner")
        except NoMatches:
            return
        inner.remove_children()
        inner.mount(*widgets)

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        labels = {"public": "Public", "private": "Private"}
        label = labels.get(visibility.lower())
        if label is None:  # fallback
            return visibility.replace("_", " ").title()
        return label
