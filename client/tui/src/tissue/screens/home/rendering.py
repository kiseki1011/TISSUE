"""Pure (no self/DOM) dashboard helpers: text clipping and small widget builders."""

from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Horizontal
from textual.widgets import DataTable, Label

from tissue.screens.home.constants import _ISSUE_KEY_WIDTH, _SEARCH_PREFIXES
from tissue.util.datetime_fmt import format_date
from tissue.widgets.issue_render import color_chip, priority_chip, type_chip

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary


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


def _issue_dash_columns() -> list[tuple[str, int | None]]:
    """Column spec for the dashboard's issue tables (My Work + issue search) — the
    same Key / Type / Title / Status / Priority / Points / Due shape the project hub's
    issue list uses, minus the Assignee column (the dashboard spans projects, so it
    has no single roster to resolve assignee names against)."""
    return [
        ("Key", _ISSUE_KEY_WIDTH),
        ("Type", 8),
        ("Title", None),
        ("Status", 13),
        ("Priority", 8),
        ("Points", 6),
        ("Due", 12),
    ]


def _issue_dash_row(
    issue: IssueSummary,
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    title_cell: str | Text,
) -> list[str | Text]:
    """One dashboard issue row, coloured like the hub: a Type chip after the Key,
    Status tinted by its workflow colour, Priority by its fixed level colour.
    `title_cell` is supplied by the caller so My Work can pass plain text and search a
    highlighted title."""
    return [
        _fit(issue.issue_key or "-", _ISSUE_KEY_WIDTH),
        type_chip(issue.issue_type_name, issue.issue_type_color),
        title_cell,
        color_chip(
            issue.current_state_label or "-",
            state_colors.get(issue.current_state_id)
            if issue.current_state_id is not None
            else None,
            pad=False,
        ),
        priority_chip(theme_variables, issue.priority),
        "-" if issue.story_point is None else str(issue.story_point),
        format_date(issue.due_at),
    ]


def _refill_table(table: DataTable, rows: list[list[str | Text]]) -> None:
    """Replace only the rows of a mounted table, keeping its columns (and header)
    so a live-search refresh doesn't flicker. `clear()` drops rows, not columns."""
    table.clear()
    for row in rows:
        table.add_row(*row)
    table.show_cursor = bool(rows)
