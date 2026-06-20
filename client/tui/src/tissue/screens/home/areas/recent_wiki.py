from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.constants import (
    _PREVIEW_COUNT,
)
from tissue.screens.home.rendering import (
    _truncate,
)
from tissue.screens.home.widgets import _DashTable
from tissue.util.datetime_fmt import format_date

log = logging.getLogger(__name__)


class RecentWikiMixin(HomeScreenBase):
    """[3] Recent Wiki box: its table and row selection."""

    def _wiki_widgets(self) -> list[Widget]:
        if self._recent_wiki is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._recent_wiki:
            return [Static("No documents yet.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            [
                Text(_truncate(d.title or "-")),
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

    @on(DataTable.RowHighlighted, "#dash-wiki")
    def _on_wiki_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_wiki(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-wiki")
    def _on_wiki_selected(self, event: DataTable.RowSelected) -> None:
        self._select_wiki(event.cursor_row)

    def _select_wiki(self, idx: int) -> None:
        if self._recent_wiki and 0 <= idx < len(self._recent_wiki):
            self.run_worker(
                self._render_wiki_detail(self._recent_wiki[idx]),
                exclusive=True,
                group="wiki-detail",
            )
