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

# Search-bar command prefixes → search kind.
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

    Top: a search bar (/project: /wiki: /issue:). Below, a 2x3 grid:
    [1] Searched Items  | Details (row-span 2)
    [2] Projects        |
    [3] Recent Wiki     | [4] My Work
    """

    CSS_PATH = "home.tcss"

    # Number keys jump to a box.
    # h/l cycle through the boxes (1→2→3→4→1).
    # j/k (and the arrows) move rows inside the focused table.
    BINDINGS = [
        Binding("1", "focus_box('dash-searched')", show=False),
        Binding("2", "focus_box('dash-projects-box')", show=False),
        Binding("3", "focus_box('dash-wiki-box')", show=False),
        Binding("4", "focus_box('dash-mywork')", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("ctrl+s", "focus_search", "search"),
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
        self._search_type: str | None = None
        self._search_results: (
            list[ProjectSummary]
            | list[WikiDocumentSearchResult]
            | list[IssueSummary]
            | None
        ) = None

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
        # Suggest the command prefixes only while typing the prefix (before the
        # ':'); once the keyword is being typed, no dropdown → Enter submits.
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

    @on(Input.Submitted, "#dashboard-search")
    def _on_search_submitted(self, event: Input.Submitted) -> None:
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

    async def _run_search(self, kind: str, keyword: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if kind == "project":
                page = await client.projects.list_projects(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                self._search_results = list(page.content or [])
            elif kind == "wiki":
                wiki_page = await client.wiki.search(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                self._search_results = list(wiki_page.content or [])
            else:  # issue
                issue_page = await client.issues.search(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                self._search_results = list(issue_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard search (%s) failed: %s", kind, e)
            self.app.notify("Search failed. Please try again.", severity="error")
            return
        self._search_type = kind
        await self._render_searched()

    def _searched_widgets(self) -> list[Widget]:
        if self._search_results is None:
            return [Static("Search above (ctrl+s)", classes="dashboard-muted")]
        if not self._search_results:
            return [Static("No results.", classes="dashboard-muted")]
        if self._search_type == "project":
            projects = cast("list[ProjectSummary]", self._search_results)
            rows: list[list[str | Text]] = [
                [
                    p.key or "-",
                    Text(self._truncate(p.title or "-")),
                    self._visibility_label(p.visibility),
                    format_date(p.created_at),
                ]
                for p in projects
            ]
            return [
                _DashTable(
                    [("Key", 6), ("Title", None), ("Visibility", 10), ("Created", 10)],
                    rows,
                    id="dash-searched-table",
                    classes="dashboard-table",
                )
            ]
        if self._search_type == "wiki":
            wikis = cast("list[WikiDocumentSearchResult]", self._search_results)
            wrows: list[list[str | Text]] = [
                [
                    Text(self._truncate(d.title or "-")),
                    format_date(d.last_modified_at),
                    format_date(d.created_at),
                ]
                for d in wikis
            ]
            return [
                _DashTable(
                    [("Title", None), ("Updated", 10), ("Created", 10)],
                    wrows,
                    id="dash-searched-table",
                    classes="dashboard-table",
                )
            ]
        # issue
        issues = cast("list[IssueSummary]", self._search_results)
        irows: list[list[str | Text]] = [
            [
                i.issue_key or "-",
                Text(self._truncate(i.title or "-")),
                i.current_state_label or "-",
                i.priority or "-",
            ]
            for i in issues
        ]
        return [
            _DashTable(
                [("Key", 9), ("Title", None), ("Status", 12), ("Pri", 4)],
                irows,
                id="dash-searched-table",
                classes="dashboard-table",
            )
        ]

    async def _render_searched(self) -> None:
        try:
            box = self.query_one("#dash-searched")
        except NoMatches:
            return
        # Await the removal: the searched table has a fixed id, so mounting a new
        # one before the old is gone would raise DuplicateIds.
        await box.remove_children()
        await box.mount(*self._searched_widgets())

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
        return [
            Static("Coming soon — issues assigned to you.", classes="dashboard-muted")
        ]

    def _projects_widgets(self) -> list[Widget]:
        if self._projects is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._projects:
            return [Static("No projects yet.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = []
        for p in self._projects:
            marker = "📌 " if self._is_pinned(p.key) else ""
            rows.append(
                [
                    p.key or "-",
                    Text(marker + self._truncate(p.title or "-")),
                    self._visibility_label(p.visibility),
                    format_date(p.created_at),
                ]
            )
        return [
            _DashTable(
                [("Key", 6), ("Title", None), ("Visibility", 10), ("Created", 10)],
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
        await self._load_dashboard()

    async def _load_dashboard(self) -> None:
        client = self.app.client
        if client is None:
            return
        # [2] Projects — every project the caller is a member of, pinned first.
        try:
            page = await client.projects.list_projects(size=100, include_archived=False)
            self._projects = list(page.content or [])
            self._sort_projects()
        except TissueApiError as e:
            log.debug("Dashboard: failed to load projects: %s", e)
            self._projects = []
        # [3] Recent Wiki — server orders by lastModified.
        try:
            wiki_page = await client.wiki.search(keyword=None, size=_PREVIEW_COUNT)
            self._recent_wiki = list(wiki_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard: failed to load recent wiki: %s", e)
            self._recent_wiki = []
        self.refresh(recompose=True)
        # Land on a data box so the box-nav keys (1-4/h/l/j/k) work right away.
        # Without this the search Input takes first focus and swallows them.
        self.call_after_refresh(self._focus_after_load)

    def _focus_after_load(self) -> None:
        focused = self.focused
        # Only steer focus off the search Input (or when nothing is focused);
        # leave the user where they are on a manual refresh.
        if focused is None or focused.id == "dashboard-search":
            self._focus_box("dash-projects-box")

    # ---- project pinning (client-side) --------------------------------

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
        self._select_project(event.cursor_row)

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

    def _select_project(self, idx: int) -> None:
        if self._projects and 0 <= idx < len(self._projects):
            self._render_project_detail(self._projects[idx])

    def _select_wiki(self, idx: int) -> None:
        if self._recent_wiki and 0 <= idx < len(self._recent_wiki):
            self.run_worker(
                self._render_wiki_detail(self._recent_wiki[idx]),
                exclusive=True,
                group="wiki-detail",
            )

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
        """Cycle focus through the boxes (l: next, h: previous)."""
        order = self._BOX_IDS
        current = self._current_box_id()
        if current not in order:  # nothing on the dashboard focused yet
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

    # ---- detail rendering ---------------------------------------------

    @staticmethod
    def _truncate(text: str, limit: int = 25) -> str:
        return text if len(text) <= limit else text[:limit] + "…"

    @staticmethod
    def _key_detail_row(value: str) -> Horizontal:
        return Horizontal(
            Label("Key:", classes="detail-key"),
            Label(Text(value), classes="detail-value dashboard-key-value"),
            classes="detail-row",
        )

    def _render_project_detail(self, p: ProjectSummary) -> None:
        self._mount_detail(
            [
                Static(p.title or "-", markup=False, classes="dashboard-detail-title"),
                self._key_detail_row(p.key or "-"),
                detail_row("Visibility", self._visibility_label(p.visibility)),
                detail_row("Created", format_relative(p.created_at)),
                detail_row("Updated", format_relative(p.last_updated_at)),
                Static(
                    p.description or "No description.",
                    markup=False,
                    classes="dashboard-detail-desc",
                ),
            ]
        )

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
