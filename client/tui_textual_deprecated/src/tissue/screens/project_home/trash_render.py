from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Markdown, Static

from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_chips import color_chip, priority_chip, type_chip

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary


def trash_detail_widgets(
    summary: IssueSummary,
    members: dict[int, str],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
) -> list[Widget]:
    """Read-only detail for a deleted issue, shared by the [2] pane and the modal.

    Built from the list summary since the detail endpoint 404s on deleted issues;
    the trash listing carries the body in `content` so the description can show.
    """
    assignee = summary.assignee_member_id
    state_color = (
        state_colors.get(summary.current_state_id)
        if summary.current_state_id is not None
        else None
    )
    widgets: list[Widget] = [
        Static(Text(summary.title or "-", style="bold"), classes="trash-title"),
        detail_row("Key", summary.issue_key or "-"),
        detail_row(
            "Status", color_chip(summary.current_state_label or "-", state_color)
        ),
        detail_row("Priority", priority_chip(theme_variables, summary.priority)),
        detail_row(
            "Type", type_chip(summary.issue_type_name, summary.issue_type_color)
        ),
        detail_row(
            "Assignee",
            members.get(assignee, "-") if assignee is not None else "-",
        ),
        detail_row(
            "Story points",
            "-" if summary.story_point is None else str(summary.story_point),
        ),
        detail_row("Due", format_relative(summary.due_at)),
        Static("Description", classes="trash-section-title"),
    ]
    content = (summary.content or "").strip()
    widgets.append(
        Markdown(content, classes="trash-content")
        if content
        else Static(Text("(empty)", style="italic"), classes="trash-muted")
    )
    return widgets
