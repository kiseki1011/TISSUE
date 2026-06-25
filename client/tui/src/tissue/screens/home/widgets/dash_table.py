from __future__ import annotations

from rich.text import Text
from textual.binding import Binding
from textual.widgets import DataTable


class _DashTable(DataTable):
    """A dashboard table that self-populates from `(columns, rows)` on mount.

    `j`/`k` move the row cursor in addition to the arrow keys.
    """

    BINDINGS = [
        Binding("j", "cursor_down", show=False),
        Binding("k", "cursor_up", show=False),
    ]

    def __init__(
        self,
        columns: list[tuple[str, int | None]],
        rows: list[list[str | Text]],
        *,
        id: str | None = None,
        classes: str | None = None,
    ) -> None:
        super().__init__(
            cursor_type="row",
            zebra_stripes=True,
            cell_padding=2,
            id=id,
            classes=classes,
        )
        self._dash_columns = columns
        self._dash_rows = rows

    def on_mount(self) -> None:
        for label, width in self._dash_columns:
            if width is None:
                self.add_column(label)
            else:
                self.add_column(label, width=width)
        for row in self._dash_rows:
            self.add_row(*row)
        self.show_cursor = bool(self._dash_rows)
