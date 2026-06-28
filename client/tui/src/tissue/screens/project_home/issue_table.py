from __future__ import annotations

from typing import TYPE_CHECKING, Any

from rich.text import Text

from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home.rendering import (
    _ISSUE_LIST_TITLE_WIDTH,
    _color_chip,
    _issue_list_rows,
)

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary

ISSUE_TABLE_ID = "hub-issues-table"
AGENT_ISSUE_TABLE_ID = "hub-agent-issues-table"
ISSUE_LIST_FOCUS_TABLE_IDS = (
    ISSUE_TABLE_ID,
    "hub-sprints-table",
    "hub-members-table",
)


def issue_table(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    member_names: dict[int, str],
) -> _DashTable:
    return _DashTable(
        [
            ("#", None),
            ("Key", 10),
            ("Type", 10),
            ("Title", _ISSUE_LIST_TITLE_WIDTH),
            ("Status", 13),
            ("Priority", 8),
            ("Points", 6),
            ("Due", 12),
            ("Assignee", 14),
        ],
        numbered_issue_rows(
            issues,
            state_colors,
            theme_variables,
            member_names,
        ),
        id=ISSUE_TABLE_ID,
        classes="hub-table",
    )


def numbered_issue_rows(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    member_names: dict[int, str],
    *,
    start: int = 0,
) -> list[list[str | Text]]:
    rows = _issue_list_rows(issues, state_colors, theme_variables, member_names)
    for index, row in enumerate(rows):
        row.insert(0, str(start + index + 1))
    return rows


def recolor_status_cells(
    panel: Any,
    table_id: str,
    issues: list[IssueSummary],
    state_colors: dict[int, str],
) -> None:
    status_col = panel.table_column_index(table_id, "Status")
    if status_col is None:
        return
    row_count = panel.table_row_count(table_id)
    for row, issue in enumerate(issues[:row_count]):
        state_id = issue.current_state_id
        if state_id is None:
            continue
        hex_color = state_colors.get(state_id)
        if hex_color:
            panel.update_table_cell(
                table_id,
                row,
                status_col,
                _color_chip(issue.current_state_label or "-", hex_color),
            )
