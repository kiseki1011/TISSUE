from __future__ import annotations

import logging
from typing import cast

from rich.text import Text
from textual import on
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import DataTable, Input, Static
from textual_autocomplete import DropdownItem, TargetState

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.issue_summary import IssueSummary
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.constants import (
    _ISSUE_KEY_WIDTH,
    _MIN_QUERY_LEN,
    _PROJECT_KEY_WIDTH,
    _SEARCH_DEBOUNCE,
    _SEARCH_PREFIXES,
    _SEARCH_SIZE,
)
from tissue.screens.home.rendering import (
    _fit,
    _parse_search,
    _refill_table,
    _truncate,
    _visibility_label,
)
from tissue.screens.home.widgets import _DashTable
from tissue.util.datetime_fmt import format_date

log = logging.getLogger(__name__)


class SearchMixin(HomeScreenBase):
    """[1] Searched Items: the search bar, debounced live search, and the
    results it produces."""

    def _search_candidates(self, state: TargetState) -> list[DropdownItem]:
        """Suggest the command prefixes only while typing the prefix."""
        if ":" in state.text:
            return []
        return [DropdownItem(prefix) for prefix in _SEARCH_PREFIXES]

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
        # typing pauses. We need a /project: /issue: prefix and a keyword
        # of at least _MIN_QUERY_LEN chars; anything shorter clears any results
        # back to the prompt. Focus stays in the input so the user keeps typing.
        self._cancel_search_timer()
        parsed = _parse_search(event.value)
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
        parsed = _parse_search(event.value)
        if parsed is None:
            self.app.notify(
                "Use /project: or /issue: followed by a keyword.",
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
        results: list[ProjectSummary] | list[IssueSummary]
        try:
            if kind == "project":
                page = await client.projects.list_projects(
                    keyword=keyword or None, size=_SEARCH_SIZE
                )
                results = list(page.content or [])
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
                    _fit(p.key or "-", _PROJECT_KEY_WIDTH),
                    self._highlight_title(p.title or "-"),
                    _visibility_label(p.visibility),
                    format_date(p.created_at),
                ]
                for p in projects
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
                _fit(i.issue_key or "-", _ISSUE_KEY_WIDTH),
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
                _refill_table(table, data[1])
                return
        # Otherwise (placeholder <-> table, or the column schema changed): a full
        # swap. The table has a fixed id, so the old one must be gone before the
        # new one mounts (else DuplicateIds).
        await box.remove_children()
        await box.mount(*self._searched_widgets())
        self._searched_table_kind = self._search_type if data is not None else None

    @on(DataTable.RowHighlighted, "#dash-searched-table")
    def _on_searched_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_searched(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-searched-table")
    def _on_searched_selected(self, event: DataTable.RowSelected) -> None:
        self._select_searched(event.cursor_row)

    def _select_searched(self, idx: int) -> None:
        results = self._search_results
        if not results or not (0 <= idx < len(results)):
            return
        item = results[idx]
        if self._search_type == "project":
            self._render_project_detail(cast("ProjectSummary", item))
        elif self._search_type == "issue":
            self._render_issue_detail(cast("IssueSummary", item))

    def _highlight_title(self, title: str) -> Text:
        """A title cell (truncated) with each case-insensitive occurrence of the
        current search keyword highlighted (bold on the primary colour, matching
        the wiki reader). No active keyword → plain text, no highlight."""
        truncated = _truncate(title)
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
