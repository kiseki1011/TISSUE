from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.constants import (
    _ISSUE_KEY_WIDTH,
)
from tissue.screens.home.rendering import (
    _fit,
    _truncate,
)
from tissue.screens.home.widgets import _DashTable

log = logging.getLogger(__name__)


class MyWorkMixin(HomeScreenBase):
    """[4] My Work box: its table and row selection."""

    def _mywork_widgets(self) -> list[Widget]:
        if self._my_work is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._my_work:
            return [Static("Nothing assigned to you.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            [
                _fit(i.issue_key or "-", _ISSUE_KEY_WIDTH),
                Text(_truncate(i.title or "-")),
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

    @on(DataTable.RowHighlighted, "#dash-mywork-table")
    def _on_mywork_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_mywork(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-mywork-table")
    def _on_mywork_selected(self, event: DataTable.RowSelected) -> None:
        self._select_mywork(event.cursor_row)

    def _select_mywork(self, idx: int) -> None:
        if self._my_work and 0 <= idx < len(self._my_work):
            self._render_issue_detail(self._my_work[idx])
