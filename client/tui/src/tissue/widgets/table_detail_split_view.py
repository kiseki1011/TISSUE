"""Reusable split view with a table on the left and a detail container on the right.

Left side: a DataTable listing items.
Right side: a Container whose contents the caller renders for the selected
item via the `detail_renderer` callback.
"""

from collections.abc import Callable
from dataclasses import dataclass

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widget import Widget
from textual.widgets import DataTable


@dataclass(frozen=True)
class Column:
    """Single column definition for the left side table.

    `width` is the fixed cell width; use `None` for content-driven sizing.
    """

    key: str
    label: str
    width: int | None = None


class TableDetailSplitView[T](Widget):
    """A split panel that shows the table on left, detail on right.

    Caller should provide:
      - `columns`: column definitions for the table
      - `row_builder`: turn one item into a row of cell strings
      - `detail_renderer`: paint the right-side container for the highlighted item

    Lifecycle:
      - compose() yields the table + container.
      - on_mount() adds the columns.
      - caller invokes populate(items) to fill rows.
      - row cursor movement triggers detail_renderer.
    """

    DEFAULT_CSS = """
    TableDetailSplitView {
        width: 100%;
        height: 100%;
        padding: 1;
    }

    TableDetailSplitView > Horizontal {
        width: 100%;
        height: 100%;
    }

    TableDetailSplitView .split-table {
        width: 1fr;
        height: 100%;
        margin-right: 1;
    }

    TableDetailSplitView .split-detail {
        width: 1fr;
        height: 100%;
        padding: 1 2;
        overflow-y: auto;
    }
    """

    def __init__(
        self,
        columns: list[Column],
        row_builder: Callable[[int, T], list[str]],
        detail_renderer: Callable[[T | None, Container], None],
        *,
        id: str | None = None,
        classes: str | None = None,
    ) -> None:
        super().__init__(id=id, classes=classes)
        self._columns = columns
        self._row_builder = row_builder
        self._detail_renderer = detail_renderer
        self._items: list[T] = []

    def compose(self) -> ComposeResult:
        with Horizontal():
            yield DataTable(
                classes="split-table panel",
                cursor_type="row",
                zebra_stripes=True,
            )
            yield Container(classes="split-detail panel")

    def on_mount(self) -> None:
        table = self.query_one(DataTable)
        for col in self._columns:
            if col.width is None:
                table.add_column(col.label, key=col.key)
            else:
                table.add_column(col.label, key=col.key, width=col.width)

    def populate(self, items: list[T]) -> None:
        """Replace the table rows and render the first item's detail."""
        self._items = list(items)
        table = self.query_one(DataTable)
        table.clear()
        # No data → hide cursor so the header line isn't highlighted.
        table.show_cursor = bool(self._items)
        for idx, item in enumerate(self._items, start=1):
            table.add_row(*self._row_builder(idx, item))
        self._render_detail(self._items[0] if self._items else None)

    def _render_detail(self, item: T | None) -> None:
        container = self.query_one(".split-detail", Container)
        self._detail_renderer(item, container)

    @on(DataTable.RowHighlighted)
    def _on_row_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if not (0 <= event.cursor_row < len(self._items)):
            return
        self._render_detail(self._items[event.cursor_row])
