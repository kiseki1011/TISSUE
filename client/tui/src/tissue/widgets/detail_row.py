from rich.text import Text
from textual.containers import Horizontal
from textual.widgets import Label


def detail_row(key: str, value: str | Text) -> Horizontal:
    """Single `key: value` row used inside detail panes.

    A plain string is wrapped in `Text` so it renders literally instead of being
    parsed as markup; a pre-built `Text` (e.g. a coloured status or a priority
    chip) is rendered as-is.
    """
    text = value if isinstance(value, Text) else Text(value)
    return Horizontal(
        Label(f"{key}:", classes="detail-key"),
        Label(text, classes="detail-value"),
        classes="detail-row",
    )
