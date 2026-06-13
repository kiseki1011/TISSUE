from collections.abc import Callable
from dataclasses import dataclass

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widgets import DataTable


@dataclass(frozen=True)
class Column:
    """Single column definition for the left side table.

    `width` is the fixed cell width. Use `None` for content-driven sizing.
    """

    key: str
    label: str
    width: int | None = None


class TableDetailSplitView[T](Container):
    """A split panel that shows the table on left, detail on right.

    Caller should provide:
      - `columns`: column definitions for the table
      - `row_builder`: turn one item into a row of cell strings
      - `detail_renderer`: paint the right-side container for the highlighted item

    Lifecycle:
      - compose() yields the table + container
      - on_mount() adds the columns and renders rows from `items`
      - caller can invoke populate(items) to replace rows dynamically
      - row cursor movement triggers detail_renderer
    """

    DEFAULT_CSS = """
    TableDetailSplitView {
        width: 100%;
        height: 100%;
        padding: 1 2;
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
        overflow-y: auto;
    }

    TableDetailSplitView .split-detail-inner {
        width: 100%;
        height: auto;
        padding: 2 4;
    }

    TableDetailSplitView .split-detail-actions {
        dock: bottom;
        width: 100%;
        height: auto;
        padding: 0 2 1 2;
    }
    """

    def __init__(
        self,
        columns: list[Column],
        row_builder: Callable[[int, T], list[str | Text]],
        detail_renderer: Callable[[T | None, Container, Container], None],
        *,
        items: list[T] | None = None,
        id: str | None = None,
        classes: str | None = None,
        table_title: str | None = None,
        detail_title: str | None = None,
    ) -> None:
        super().__init__(id=id, classes=classes)
        self._columns = columns
        self._row_builder = row_builder
        self._detail_renderer = detail_renderer
        self._items: list[T] = list(items) if items else []
        self._table_title = table_title
        self._detail_title = detail_title

    def compose(self) -> ComposeResult:
        table = DataTable(
            classes="split-table panel",
            cursor_type="row",
            zebra_stripes=True,
        )
        if self._table_title:
            table.border_title = self._table_title

        detail = Container(
            Container(classes="split-detail-inner"),
            Container(classes="split-detail-actions"),
            classes="split-detail panel",
        )
        if self._detail_title:
            detail.border_title = self._detail_title

        with Horizontal():
            yield table
            yield detail

    def on_mount(self) -> None:
        table = self.query_one(DataTable)
        for col in self._columns:
            if col.width is None:
                table.add_column(col.label, key=col.key)
            else:
                table.add_column(col.label, key=col.key, width=col.width)
        self._render_table()

    def populate(self, items: list[T]) -> None:
        """Replace the table rows and render the first item's detail."""
        self._items = list(items)
        self._render_table()

    def _render_table(self) -> None:
        table = self.query_one(DataTable)
        table.clear()
        # Prevent header line highlight when no data
        table.show_cursor = bool(self._items)
        for idx, item in enumerate(self._items, start=1):
            table.add_row(*self._row_builder(idx, item))
        self._render_detail(self._items[0] if self._items else None)

    def _render_detail(self, item: T | None) -> None:
        content = self.query_one(".split-detail-inner", Container)
        actions = self.query_one(".split-detail-actions", Container)
        self._detail_renderer(item, content, actions)

    @on(DataTable.RowHighlighted)
    def _on_row_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if not (0 <= event.cursor_row < len(self._items)):
            return
        self._render_detail(self._items[event.cursor_row])
