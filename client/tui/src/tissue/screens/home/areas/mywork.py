from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.panels import MyWorkPanel
from tissue.screens.home.rendering import (
    _issue_dash_columns,
    _issue_dash_row,
    _truncate,
)
from tissue.screens.home.widgets import _DashTable

log = logging.getLogger(__name__)


class MyWorkMixin(HomeScreenBase):
    """The [2] My Work box, its issue table and row selection.

    The table mirrors the project hub colors across the columns Key, Type,
    Title, Status, Priority, Points, and Due.
    """

    def _mywork_widgets(self) -> list[Widget]:
        my_work = self._my_work.items
        if my_work is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not my_work:
            return [Static("Nothing assigned to you.", classes="dashboard-muted")]
        rows: list[list[str | Text]] = [
            _issue_dash_row(
                issue,
                self._workflows.state_colors,
                self.app.theme_variables,
                Text(_truncate(issue.title or "-", 13)),
            )
            for issue in my_work
        ]
        return [
            _DashTable(
                _issue_dash_columns(),
                rows,
                id=MyWorkPanel.TABLE_ID,
                classes="dashboard-table",
            )
        ]

    @on(DataTable.RowHighlighted, f"#{MyWorkPanel.TABLE_ID}")
    def _on_mywork_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_mywork(event.cursor_row)

    @on(DataTable.RowSelected, f"#{MyWorkPanel.TABLE_ID}")
    def _on_mywork_selected(self, event: DataTable.RowSelected) -> None:
        self._select_mywork(event.cursor_row)

    def _select_mywork(self, index: int) -> None:
        my_work = self._my_work.items
        if not (my_work and 0 <= index < len(my_work)):
            return
        issue_key = my_work[index].issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key), exclusive=True, group="dash-detail"
        )
