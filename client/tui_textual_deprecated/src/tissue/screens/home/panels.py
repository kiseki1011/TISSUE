from __future__ import annotations

from rich.text import Text
from textual.app import ComposeResult
from textual.containers import Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, DataTable, Input, Static

from tissue.screens.home.rendering import _refill_table


class DashboardSearchBar(Horizontal):
    def __init__(self, search: Input) -> None:
        super().__init__(id="dashboard-search-row")
        self._search = search

    def compose(self) -> ComposeResult:
        yield self._search
        yield Button("⚙", id="dashboard-filter", classes="search-filter-btn")


class DashboardBox(Vertical):
    def __init__(self, title: str, box_id: str, children: list[Widget]) -> None:
        super().__init__(*children, id=box_id, classes="dashboard-box panel")
        self.border_title = title

    async def replace_content(self, widgets: list[Widget]) -> None:
        with self.app.batch_update():
            await self.remove_children()
            await self.mount(*widgets)

    def table(self, table_id: str) -> DataTable | None:
        try:
            return self.query_one(f"#{table_id}", DataTable)
        except NoMatches:
            return None

    def first_table(self) -> DataTable | None:
        return next(iter(self.query(DataTable)), None)

    def focus_table_or_box(self) -> int | None:
        table = self.first_table()
        if table is not None:
            self.can_focus = False
            table.focus()
            return table.cursor_row
        self.can_focus = True
        self.focus()
        return None

    def table_cursor_row(self, table_id: str) -> int | None:
        table = self.table(table_id)
        return None if table is None else table.cursor_row

    def table_row_count(self, table_id: str) -> int:
        table = self.table(table_id)
        return 0 if table is None else table.row_count

    def move_table_cursor(self, table_id: str, row: int) -> bool:
        table = self.table(table_id)
        if table is None:
            return False
        table.move_cursor(row=row, animate=False)
        return True

    def refill_table(self, table_id: str, rows: list[list[str | Text]]) -> bool:
        table = self.table(table_id)
        if table is None:
            return False
        _refill_table(table, rows)
        return True


class SearchResultsPanel(DashboardBox):
    BOX_ID = "dash-searched"
    TABLE_ID = "dash-searched-table"

    def __init__(self, children: list[Widget]) -> None:
        super().__init__("[1] Searched Items", self.BOX_ID, children)


class MyWorkPanel(DashboardBox):
    BOX_ID = "dash-mywork"
    TABLE_ID = "dash-mywork-table"

    def __init__(self, children: list[Widget]) -> None:
        super().__init__("[2] My Work", self.BOX_ID, children)


class ProjectsPanel(DashboardBox):
    BOX_ID = "dash-projects-box"
    TABLE_ID = "dash-projects"

    def __init__(self, children: list[Widget]) -> None:
        super().__init__("[3] Projects", self.BOX_ID, children)


class DashboardDetailPanel(VerticalScroll):
    def __init__(self) -> None:
        super().__init__(id="dashboard-detail", classes="dashboard-box")
        self.border_title = "Details"
        self.can_focus = False

    def compose(self) -> ComposeResult:
        yield Vertical(
            Static("Select an item to see details.", classes="dashboard-muted"),
            id="dashboard-detail-inner",
        )

    def replace_content(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#dashboard-detail-inner", Vertical)
        except NoMatches:
            return
        with self.app.batch_update():
            inner.remove_children()
            inner.mount(*widgets)
