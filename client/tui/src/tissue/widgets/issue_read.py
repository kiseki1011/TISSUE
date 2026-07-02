from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_chips import (
    color_chip,
    priority_chip,
    review_status_chip,
    type_text,
)
from tissue.widgets.issue_fields import (
    custom_field_section,
    member_name,
    progress_block,
)
from tissue.widgets.issue_refs import hierarchy_read_block, relations_read_block

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )


def issue_edit_current(detail: IssueCommonDetail) -> dict[str, str]:
    """String-form field values the edit modal pre-fills from."""
    return {
        "title": detail.title or "",
        "priority": detail.priority or "",
        "dueAt": detail.due_at.isoformat() if detail.due_at else "",
        "storyPoint": "" if detail.story_point is None else str(detail.story_point),
        "content": detail.content or "",
    }


def reviewer_read_block(
    detail: IssueCommonDetail, theme_variables: dict[str, str]
) -> list[Widget]:
    reviewers = detail.reviewers or []
    if not reviewers:
        return []
    widgets: list[Widget] = [
        Static("", classes="detail-gap"),
        Static(Text("Reviewers", style="bold")),
    ]
    for reviewer in reviewers:
        widgets.append(
            detail_row(
                member_name(reviewer.participant),
                review_status_chip(theme_variables, reviewer.status),
            )
        )
    return widgets


def issue_read_view(
    detail: IssueCommonDetail,
    custom_fields: list[CustomFieldValueInfo],
    options_by_field: dict[int, list[FieldOptionDetail]],
    theme_variables: dict[str, str],
    *,
    title_class: str = "detail-title",
    content_class: str = "detail-content",
    muted_class: str = "detail-muted",
    show_reviewers: bool = False,
    parent: IssueIdentifierResponse | None = None,
    children: list[IssueIdentifierResponse] | None = None,
    relations: IssueRelationsDetail | None = None,
) -> list[Widget]:
    state = detail.current_state
    current_state_label = (state.display_name if state else None) or "-"
    widgets: list[Widget] = [
        Static(detail.title or "-", markup=False, classes=title_class),
        detail_row("Key", detail.issue_key or "-"),
        detail_row(
            "Status", color_chip(current_state_label, state.color if state else None)
        ),
        detail_row("Priority", priority_chip(theme_variables, detail.priority)),
        detail_row("Type", type_text(detail.issue_type)),
        detail_row("Assignee", member_name(detail.assignee)),
        detail_row("Author", member_name(detail.author)),
        detail_row(
            "Story points",
            "-" if detail.story_point is None else str(detail.story_point),
        ),
        *progress_block(detail),
        detail_row("Due", format_relative(detail.due_at)),
        detail_row("Created", format_relative(detail.created_at)),
        detail_row("Updated", format_relative(detail.last_updated_at)),
        *custom_field_section(custom_fields, options_by_field),
        *(reviewer_read_block(detail, theme_variables) if show_reviewers else []),
        *hierarchy_read_block(parent, children),
        *relations_read_block(relations),
        Rule(),
    ]
    content = (detail.content or "").strip()
    widgets.append(
        Markdown(content, classes=content_class)
        if content
        else Static(Text("(empty)", style="italic"), classes=muted_class)
    )
    return widgets
