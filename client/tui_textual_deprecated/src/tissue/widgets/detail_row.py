from rich.text import Text
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Label


def detail_row(
    key: str, value: str | Text, *, action: Widget | None = None
) -> Horizontal:
    """Single `key: value` row used inside detail panes.

    A plain string is wrapped in `Text` so it renders literally instead of being
    parsed as markup.
    """
    text = value if isinstance(value, Text) else Text(value)
    children: list[Widget] = [
        Label(f"{key}:", classes="detail-key"),
        Label(text, classes="detail-value"),
    ]
    if action is not None:
        children.append(action)
    return Horizontal(*children, classes="detail-row")
