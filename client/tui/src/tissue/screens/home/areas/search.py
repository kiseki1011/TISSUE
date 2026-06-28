from __future__ import annotations

import logging
from typing import cast

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Input, Static
from textual_autocomplete import DropdownItem, TargetState

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.issue_summary import IssueSummary
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.constants import (
    _MIN_QUERY_LEN,
    _PROJECT_KEY_WIDTH,
    _SEARCH_DEBOUNCE,
    _SEARCH_PREFIXES,
    _SEARCH_SIZE,
)
from tissue.screens.home.panels import SearchResultsPanel
from tissue.screens.home.rendering import (
    _fit,
    _issue_dash_columns,
    _issue_dash_row,
    _parse_search,
    _truncate,
    _visibility_label,
)
from tissue.screens.home.widgets import _DashTable
from tissue.util.datetime_fmt import format_date
from tissue.widgets.color_type import color_hex

log = logging.getLogger(__name__)


class SearchMixin(HomeScreenBase):
    """[1] Searched Items.

    The search bar, debounced live search, and results.
    """

    def _search_candidates(self, state: TargetState) -> list[DropdownItem]:
        """Suggest the command prefixes only while typing the prefix."""
        if ":" in state.text:
            return []
        return [DropdownItem(prefix) for prefix in _SEARCH_PREFIXES]

    def _cancel_search_timer(self) -> None:
        if self._search.timer is not None:
            self._search.timer.stop()
            self._search.timer = None

    def on_unmount(self) -> None:
        # Don't let a pending debounce fire on a screen that's going away.
        self._cancel_search_timer()

    @on(Input.Changed, "#dashboard-search")
    def _on_search_changed(self, event: Input.Changed) -> None:
        # Restart the debounce timer so the search fires only once typing pauses.
        # A keyword below _MIN_QUERY_LEN clears results back to the prompt.
        self._cancel_search_timer()
        parsed = _parse_search(event.value)
        if parsed is None or len(parsed[1]) < _MIN_QUERY_LEN:
            self._reset_search()
            return
        kind, keyword = parsed
        self._search.timer = self.set_timer(
            _SEARCH_DEBOUNCE,
            lambda: self.run_worker(
                self._run_search(kind, keyword),
                exclusive=True,
                group="dash-search",
            ),
        )

    @on(Input.Submitted, "#dashboard-search")
    def _on_search_submitted(self, event: Input.Submitted) -> None:
        # Enter searches immediately. Unlike live search it allows an empty
        # keyword, which browses everything for that kind.
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
        """Clear the Searched Items box back to its prompt.

        Called when the keyword falls below the minimum length.
        """
        # Raise the version counter first, unconditionally. An in-flight search
        # may not have assigned its results yet, but it must still be invalidated
        # so it can't render results for a query the user has already backspaced
        # away. _run_search re-checks the counter after its await and drops the
        # stale result.
        self._search.invalidate()
        if self._search.kind is None and self._search.results is None:
            # Nothing on screen to clear, so skip the re-render and avoid
            # rebuilding the box on every keystroke while typing the prefix.
            return
        self._search.clear_results()
        self.run_worker(self._render_searched(), exclusive=True, group="dash-search")

    async def _run_search(self, kind: str, keyword: str) -> None:
        generation = self._search.invalidate()
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
        except TissueApiError as error:
            log.debug("Dashboard search (%s) failed: %s", kind, error)
            self.app.notify("Search failed. Please try again.", severity="error")
            return
        if generation != self._search.generation:
            return
        self._search.results = results
        self._search.kind = kind
        self._search.keyword = keyword
        await self._render_searched()

    def _searched_table_data(
        self,
    ) -> tuple[list[tuple[str, int | None]], list[list[str | Text]]] | None:
        """The (columns, rows) for the current results, or None for a placeholder.

        Returns None when a placeholder Static should be shown instead, either
        not yet searched or no results. Columns depend only on the search kind,
        so two calls with the same search kind always return the same column
        spec.
        """
        results = self._search.results
        if not results:  # None (not searched) or empty (no matches)
            return None
        if self._search.kind == "project":
            projects = cast("list[ProjectSummary]", results)
            columns: list[tuple[str, int | None]] = [
                ("Key", _PROJECT_KEY_WIDTH),
                ("Title", None),
                ("Visibility", 10),
                ("Created", 10),
            ]
            rows: list[list[str | Text]] = [
                [
                    _fit(project.key or "-", _PROJECT_KEY_WIDTH),
                    self._highlight_title(project.title or "-"),
                    _visibility_label(project.visibility),
                    format_date(project.created_at),
                ]
                for project in projects
            ]
            return columns, rows
        # issue
        issues = cast("list[IssueSummary]", results)
        columns = _issue_dash_columns()
        rows = [
            _issue_dash_row(
                issue,
                self._workflows.state_colors,
                self.app.theme_variables,
                self._highlight_title(issue.title or "-", 13),
            )
            for issue in issues
        ]
        return columns, rows

    def _searched_widgets(self) -> list[Widget]:
        if self._search.results is None:
            return [Static("Search above (ctrl+/)", classes="dashboard-muted")]
        data = self._searched_table_data()
        if data is None:  # searched, but no matches
            return [Static("No results.", classes="dashboard-muted")]
        columns, rows = data
        return [
            _DashTable(
                columns,
                rows,
                id=SearchResultsPanel.TABLE_ID,
                classes="dashboard-table",
            )
        ]

    async def _render_searched(self) -> None:
        box = self._dashboard_box(SearchResultsPanel.BOX_ID)
        if box is None:
            return
        data = self._searched_table_data()
        # When the search kind is unchanged and a table is already mounted, only
        # the rows differ. Refill them in place, since remounting the table would
        # flicker the column header on every keystroke.
        if (
            data is not None
            and self._search.kind == self._search.mounted_table_kind
            and box.refill_table(SearchResultsPanel.TABLE_ID, data[1])
        ):
            return
        await box.replace_content(self._searched_widgets())
        self._search.mounted_table_kind = (
            self._search.kind if data is not None else None
        )

    @on(DataTable.RowHighlighted, f"#{SearchResultsPanel.TABLE_ID}")
    def _on_searched_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_searched(event.cursor_row)

    @on(DataTable.RowSelected, f"#{SearchResultsPanel.TABLE_ID}")
    def _on_searched_selected(self, event: DataTable.RowSelected) -> None:
        self._select_searched(event.cursor_row)

    def _select_searched(self, idx: int) -> None:
        results = self._search.results
        if not results or not (0 <= idx < len(results)):
            return
        item = results[idx]
        if self._search.kind == "project":
            self._render_project_detail(cast("ProjectSummary", item))
        elif self._search.kind == "issue":
            issue_key = cast("IssueSummary", item).issue_key
            if issue_key is not None:
                self.run_worker(
                    self._render_issue_detail(issue_key),
                    exclusive=True,
                    group="dash-detail",
                )

    def _highlight_title(self, title: str, limit: int = 18) -> Text:
        """A title cell (truncated to `limit`) with the keyword highlighted.

        Each case-insensitive occurrence of the current search keyword is shown
        bold on the primary color, matching the wiki reader. With no active
        keyword the text is returned plain.
        """
        truncated = _truncate(title, limit)
        keyword = self._search.keyword
        text = Text()
        if not keyword:
            text.append(truncated)
            return text
        # color_hex() turns the theme's primary into a #hex value. ANSI themes
        # expose it as an `ansi_*` name that Rich's Text style parser rejects and
        # crashes on, and color_hex returns "" when it can't, so fall back to
        # plain reverse.
        primary = color_hex(self.app.theme_variables.get("primary"))
        match_style = f"bold on {primary}" if primary else "bold reverse"
        lowered = truncated.casefold()
        lowered_keyword = keyword.casefold()
        start = 0
        while True:
            match = lowered.find(lowered_keyword, start)
            if match == -1:
                text.append(truncated[start:])
                return text
            if match > start:
                text.append(truncated[start:match])
            text.append(truncated[match : match + len(keyword)], style=match_style)
            start = match + len(keyword)
