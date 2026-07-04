from __future__ import annotations

import asyncio
from typing import Any

from textual.app import ComposeResult
from textual.containers import Horizontal, Vertical, VerticalScroll
from textual.coordinate import Coordinate
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, DataTable, Input, Static


class ProjectSearchBar(Horizontal):
    def __init__(self) -> None:
        super().__init__(id="hub-search-row")

    def compose(self) -> ComposeResult:
        search = Input(placeholder="Search issues…", id="hub-search")
        search.border_title = "Search"
        filter_btn = Button("⚙", id="hub-filter", classes="search-filter-btn")
        filter_btn.tooltip = "Filter issues"
        yield search
        yield filter_btn
        yield Button("+", id="hub-new-issue", classes="search-filter-btn")


class _HostPanel(Vertical):
    _host_id: str

    async def replace_content(self, widgets: list[Widget]) -> None:
        host = self.query_one(f"#{self._host_id}", Vertical)
        with self.app.batch_update():
            await host.remove_children()
            await host.mount(*widgets)

    def focus_host(self) -> None:
        self.query_one(f"#{self._host_id}", Vertical).focus()

    def table(self, table_id: str) -> DataTable | None:
        try:
            return self.query_one(f"#{table_id}", DataTable)
        except NoMatches:
            return None

    def focus_first_table(self, table_ids: tuple[str, ...]) -> bool:
        for table_id in table_ids:
            table = self.table(table_id)
            if table is not None:
                table.focus()
                return True
        return False

    def has_focus_in(self, widget_ids: set[str]) -> bool:
        focused = self.app.focused
        return focused is not None and focused.id in widget_ids

    def add_table_rows(self, table_id: str, rows: list[list[Any]]) -> bool:
        table = self.table(table_id)
        if table is None:
            return False
        for row in rows:
            table.add_row(*row)
        return True

    def table_near_bottom(self, table_id: str, *, threshold: int) -> bool:
        table = self.table(table_id)
        if table is None:
            return False
        return table.max_scroll_y > 0 and table.scroll_offset.y >= (
            table.max_scroll_y - threshold
        )

    def table_column_index(self, table_id: str, label: str) -> int | None:
        table = self.table(table_id)
        if table is None:
            return None
        return next(
            (
                index
                for index, column in enumerate(table.columns.values())
                if str(column.label) == label
            ),
            None,
        )

    def update_table_cell(
        self, table_id: str, row: int, column: int, value: Any
    ) -> None:
        table = self.table(table_id)
        if table is not None and row < table.row_count:
            table.update_cell_at(Coordinate(row, column), value)

    def table_cursor_row(
        self, table_id: str, *, require_focus: bool = False
    ) -> int | None:
        table = self.table(table_id)
        if table is None or (require_focus and not table.has_focus):
            return None
        return table.cursor_row

    def table_row_count(self, table_id: str) -> int:
        table = self.table(table_id)
        return 0 if table is None else table.row_count


class IssueListPanel(_HostPanel):
    def __init__(self) -> None:
        super().__init__(id="hub-issues-box", classes="hub-box panel")
        self._host_id = "hub-list-host"
        self.border_title = "[1] Issues"
        self.border_subtitle = "Switch: < >"

    def compose(self) -> ComposeResult:
        yield _ListHost(self._host_id)


class IssueDetailPanel(Horizontal):
    def __init__(self) -> None:
        super().__init__(id="hub-detail")
        self.border_title = "[2] Details"
        self.border_subtitle = "Hide: CTRL+F"
        self._body_lock = asyncio.Lock()

    def compose(self) -> ComposeResult:
        main = VerticalScroll(
            Vertical(
                Static("Select an issue to see details.", classes="hub-muted"),
                id="hub-detail-main-inner",
            ),
            id="hub-detail-main",
        )
        main.can_focus = True
        yield main

    async def replace_body(self, widgets: list[Widget]) -> None:
        async def swap() -> None:
            async with self._body_lock:
                inner = self.query_one("#hub-detail-main-inner", Vertical)
                with self.app.batch_update():
                    await inner.remove_children()
                    await inner.mount(*widgets)

        await asyncio.shield(swap())

    def focus_body(self) -> None:
        self.query_one("#hub-detail-main", VerticalScroll).focus()

    def body_has_focus(self) -> bool:
        return self.app.focused is self.query_one("#hub-detail-main", VerticalScroll)

    def scroll_body(self, direction: str) -> None:
        body = self.query_one("#hub-detail-main", VerticalScroll)
        if direction == "down":
            body.scroll_down()
        else:
            body.scroll_up()

    def table_cursor_row(self, table_id: str) -> int | None:
        try:
            return self.query_one(f"#{table_id}", DataTable).cursor_row
        except NoMatches:
            return None


class ActivityPanel(Vertical):
    """[3] the issue's activity timeline, a box the user can close to widen [2]."""

    def __init__(self) -> None:
        super().__init__(id="hub-activity")
        self.border_title = "[3] Activity"
        self.border_subtitle = "Hide: CTRL+W"
        self._lock = asyncio.Lock()

    def compose(self) -> ComposeResult:
        scroll = VerticalScroll(
            Vertical(id="hub-activity-inner"),
            id="hub-activity-scroll",
        )
        scroll.can_focus = True
        yield scroll

    async def replace(self, widgets: list[Widget]) -> None:
        async def swap() -> None:
            async with self._lock:
                inner = self.query_one("#hub-activity-inner", Vertical)
                with self.app.batch_update():
                    await inner.remove_children()
                    await inner.mount(*widgets)

        await asyncio.shield(swap())

    async def clear(self) -> None:
        async def _clear() -> None:
            async with self._lock:
                inner = self.query_one("#hub-activity-inner", Vertical)
                await inner.remove_children()

        await asyncio.shield(_clear())

    def focus_scroll(self) -> None:
        self.query_one("#hub-activity-scroll", VerticalScroll).focus()

    def has_focus_in(self) -> bool:
        scroll = self.query_one("#hub-activity-scroll", VerticalScroll)
        return self.app.focused is scroll

    def scroll_activity(self, direction: str) -> None:
        scroll = self.query_one("#hub-activity-scroll", VerticalScroll)
        if direction == "down":
            scroll.scroll_down()
        else:
            scroll.scroll_up()


class _ListHost(Vertical):
    def __init__(self, widget_id: str) -> None:
        super().__init__(id=widget_id, classes="hub-list-host")
        self.can_focus = True

    def compose(self) -> ComposeResult:
        yield Static("Loading…", classes="hub-muted")
