from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_chips import (
    color_chip as _color_chip,
)
from tissue.widgets.issue_chips import (
    priority_chip as _priority_chip,
)
from tissue.widgets.issue_chips import (
    type_chip as _type_chip,
)
from tissue.widgets.issue_chips import (
    type_text as _type_text,
)
from tissue.widgets.issue_fields import (
    custom_field_section,
    progress_block,
)
from tissue.widgets.issue_fields import (
    member_name as _member_name,
)
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary

log = logging.getLogger(__name__)


def _edit_button(button_id: str) -> TextButton:
    return TextButton("✎", id=button_id, classes="hub-row-action hub-field-edit")


class DetailRenderMixin(ProjectHomeBase):
    """Builds the [2] detail-pane widgets from a loaded issue."""

    def _summary_for(self, issue_key: str) -> IssueSummary | None:
        for summary in (*self._issue_list.issues, *self._agent_work.issues):
            if summary.issue_key == issue_key:
                return summary
        return None

    def _skeleton_widgets(self, summary: IssueSummary) -> list[Widget]:
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._member_list.members
            if member.member_id is not None
        }
        return [
            Horizontal(
                Static(summary.title or "-", markup=False, classes="hub-detail-title"),
                classes="hub-title-row",
            ),
            detail_row("Key", summary.issue_key or "-"),
            detail_row(
                "Status",
                _color_chip(
                    summary.current_state_label or "-",
                    self._state_colors.get(summary.current_state_id)
                    if summary.current_state_id is not None
                    else None,
                ),
            ),
            detail_row(
                "Priority", _priority_chip(self.app.theme_variables, summary.priority)
            ),
            detail_row(
                "Type",
                _type_chip(summary.issue_type_name, summary.issue_type_color),
            ),
            detail_row(
                "Assignee",
                member_names.get(summary.assignee_member_id, "-")
                if summary.assignee_member_id is not None
                else "-",
            ),
        ]

    def _cf_edit_button(self, field_id: int) -> TextButton:
        return TextButton(
            "✎", id=f"hub-cf-edit-{field_id}", classes="hub-row-action hub-cf-edit"
        )

    def _safe_issue_widgets(
        self,
        issue: IssueCommonDetail,
        transitions: list[AvailableTransition],
        target_labels: dict[int, str],
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        comments: list[CommentDetailResponse],
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        try:
            return self._issue_widgets(
                issue,
                transitions,
                target_labels,
                custom_fields,
                options_by_field,
                comments,
                parent,
                children,
            )
        except Exception:
            log.exception("Hub: failed to build issue detail for %s", issue.issue_key)
            return [Static("Couldn't render this issue.", classes="hub-muted")]

    def _issue_widgets(
        self,
        detail: IssueCommonDetail,
        transitions: list[AvailableTransition],
        target_labels: dict[int, str],
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        comments: list[CommentDetailResponse],
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        state = detail.current_state
        issue_type = detail.issue_type
        current_state_label = (state.display_name if state else None) or "-"
        self._detail_state.edit_current = {
            "title": detail.title or "",
            "priority": detail.priority or "",
            "dueAt": detail.due_at.isoformat() if detail.due_at else "",
            "storyPoint": "" if detail.story_point is None else str(detail.story_point),
            "content": detail.content or "",
        }
        widgets: list[Widget] = [
            Horizontal(
                Static(detail.title or "-", markup=False, classes="hub-detail-title"),
                _edit_button("hub-edit-title"),
                classes="hub-title-row",
            ),
            detail_row("Key", detail.issue_key or "-"),
            detail_row(
                "Status",
                _color_chip(current_state_label, state.color if state else None),
                action=self._status_action(
                    transitions, current_state_label, target_labels
                ),
            ),
            detail_row(
                "Priority",
                _priority_chip(self.app.theme_variables, detail.priority),
                action=_edit_button("hub-edit-priority"),
            ),
            detail_row("Type", _type_text(issue_type)),
            self._current_sprint_row(detail.issue_key),
            detail_row(
                "Assignee",
                _member_name(detail.assignee),
                action=TextButton(
                    "✎", id="hub-assignee-edit", classes="hub-row-action"
                ),
            ),
            detail_row("Author", _member_name(detail.author)),
            detail_row(
                "Story points",
                "-" if detail.story_point is None else str(detail.story_point),
                action=_edit_button("hub-edit-sp"),
            ),
            *progress_block(detail),
            detail_row(
                "Due",
                format_relative(detail.due_at),
                action=_edit_button("hub-edit-due"),
            ),
            detail_row("Created", format_relative(detail.created_at)),
            detail_row("Updated", format_relative(detail.last_updated_at)),
            *custom_field_section(
                custom_fields, options_by_field, edit_button=self._cf_edit_button
            ),
            *self._reviewer_section(detail),
            *self._hierarchy_section(detail, parent, children),
            *self._relations_section(detail),
            Rule(),
            Horizontal(
                TextButton(
                    "✎",
                    id="hub-edit-description",
                    classes="hub-row-action hub-desc-edit",
                ),
                classes="hub-desc-header",
            ),
        ]
        content = (detail.content or "").strip()
        widgets.append(
            Markdown(content, classes="hub-content")
            if content
            else Static(Text("(empty)", style="italic"), classes="hub-muted")
        )
        widgets.extend(self._comment_section(comments))
        return widgets

    def _current_sprint_row(self, issue_key: str | None) -> Widget:
        summary = self._summary_for(issue_key) if issue_key else None
        index = self._sprint_state.by_id or {}
        current = (
            index.get(summary.sprint_id)
            if summary is not None and summary.sprint_id is not None
            else None
        )
        name = (current.sprint_key or current.title or "-") if current else "-"
        active = self._active_sprint()
        add_button = TextButton("+", id="hub-add-to-sprint", classes="hub-row-action")
        add_button.disabled = active is None
        add_button.tooltip = (
            f"Add this issue to {active.sprint_key}"
            if active is not None
            else "No active sprint to add to"
        )
        return detail_row("Current Sprint", name, action=add_button)
