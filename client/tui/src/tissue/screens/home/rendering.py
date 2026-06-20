"""Pure (no self/DOM) dashboard helpers: text clipping and small widget builders."""

from __future__ import annotations

from rich.text import Text
from textual.containers import Horizontal
from textual.widgets import DataTable, Label

from tissue.screens.home.constants import _SEARCH_PREFIXES


def _parse_search(raw: str) -> tuple[str, str] | None:
    """Split a `/project:foo` style query into (kind, keyword); None if no prefix."""
    raw = raw.strip()
    for prefix, kind in _SEARCH_PREFIXES.items():
        if raw.startswith(prefix):
            return kind, raw[len(prefix) :].strip()
    return None


def _truncate(text: str, limit: int = 25) -> str:
    return text if len(text) <= limit else text[:limit] + "…"


def _fit(text: str, width: int) -> str:
    """Clip to a fixed column width, marking overflow with a trailing ellipsis."""
    return text if len(text) <= width else text[: width - 1] + "…"


def _visibility_label(visibility: str | None) -> str:
    if not visibility:
        return "-"
    labels = {"public": "Public", "private": "Private"}
    label = labels.get(visibility.lower())
    if label is None:  # fallback
        return visibility.replace("_", " ").title()
    return label


def _key_detail_row(value: str) -> Horizontal:
    return Horizontal(
        Label("Key:", classes="detail-key"),
        Label(Text(value), classes="detail-value dashboard-key-value"),
        classes="detail-row",
    )


def _refill_table(table: DataTable, rows: list[list[str | Text]]) -> None:
    """Replace only the rows of a mounted table, keeping its columns (and header)
    so a live-search refresh doesn't flicker. `clear()` drops rows, not columns."""
    table.clear()
    for row in rows:
        table.add_row(*row)
    table.show_cursor = bool(rows)
