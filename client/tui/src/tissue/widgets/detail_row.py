from rich.text import Text
from textual.containers import Horizontal
from textual.widgets import Label


def detail_row(key: str, value: str) -> Horizontal:
    """Single `key: value` row used inside detail panes.

    The value is wrapped in `Text` so free text renders instead of being
    parsed as markup.
    """
    return Horizontal(
        Label(f"{key}:", classes="detail-key"),
        Label(Text(value), classes="detail-value"),
        classes="detail-row",
    )
