from textual.containers import Horizontal
from textual.widgets import Label


def detail_row(key: str, value: str) -> Horizontal:
    """Single `key: value` row used inside detail panes."""
    return Horizontal(
        Label(f"{key}:", classes="detail-key"),
        Label(value, classes="detail-value"),
        classes="detail-row",
    )
