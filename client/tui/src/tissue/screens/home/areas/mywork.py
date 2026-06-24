from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.rendering import (
    _issue_dash_columns,
    _issue_dash_row,
    _truncate,
)
from tissue.screens.home.widgets import _DashTable

log = logging.getLogger(__name__)


class MyWorkMixin(HomeScreenBase):
    """[2] My Work box: its table (Key/Type/Title/Status/Priority/Points/Due,
    coloured like the project hub) and row selection."""

    def _mywork_widgets(self) -> list[Widget]:
        if self._my_work is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._my_work:
            return [Static("Nothing assigned to you.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            _issue_dash_row(
                i,
                self._state_colors,
                self.app.theme_variables,
                Text(_truncate(i.title or "-", 13)),
            )
            for i in self._my_work
        ]
        return [
            _DashTable(
                _issue_dash_columns(),
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
        if not (self._my_work and 0 <= idx < len(self._my_work)):
            return
        issue_key = self._my_work[idx].issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key), exclusive=True, group="dash-detail"
        )
