from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.markdown import Markdown as RichMarkdown
from rich.text import Text
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Rule, Static

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
from tissue.widgets.issue_read import issue_edit_current

if TYPE_CHECKING:
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

    def _safe_issue_widgets(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        comments: list[CommentDetailResponse],
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        try:
            return self._issue_widgets(
                issue,
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
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        comments: list[CommentDetailResponse],
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        state = detail.current_state
        issue_type = detail.issue_type
        current_state_label = (state.display_name if state else None) or "-"
        self._detail_state.edit_current = issue_edit_current(detail)
        widgets: list[Widget] = [
            Horizontal(
                Static(detail.title or "-", markup=False, classes="hub-detail-title"),
                classes="hub-title-row",
            ),
            detail_row("Key", detail.issue_key or "-"),
            detail_row(
                "Status",
                _color_chip(current_state_label, state.color if state else None),
            ),
            detail_row(
                "Priority",
                _priority_chip(self.app.theme_variables, detail.priority),
            ),
            detail_row("Type", _type_text(issue_type)),
            self._current_sprint_row(detail.issue_key),
            detail_row("Assignee", _member_name(detail.assignee)),
            detail_row("Author", _member_name(detail.author)),
            detail_row(
                "Story points",
                "-" if detail.story_point is None else str(detail.story_point),
            ),
            *progress_block(detail),
            detail_row("Due", format_relative(detail.due_at)),
            detail_row("Created", format_relative(detail.created_at)),
            detail_row("Updated", format_relative(detail.last_updated_at)),
            *custom_field_section(custom_fields, options_by_field),
            *self._reviewer_section(detail),
            *self._hierarchy_section(detail, parent, children),
            *self._relations_section(detail),
            Rule(),
            Static("Description", classes="hub-section-title"),
        ]
        content = (detail.content or "").strip()
        # Rich's Markdown renders as ONE Static renderable (~0.8ms) vs Textual's
        # Markdown widget, which builds a whole sub-widget tree (~10ms) and stutters
        # held-key list navigation as [2] re-renders per row. The read modal keeps
        # the interactive widget; this preview pane only needs formatted, fast text.
        widgets.append(
            Static(RichMarkdown(content), classes="hub-content")
            if content
            else Static(Text("(empty)", style="italic"), classes="hub-muted")
        )
        widgets.extend(self._comment_section(comments))
        return widgets

    def _current_sprint_row(self, issue_key: str | None) -> Widget:
        # No inline button — like Status/Priority/Assignee, sprint membership is
        # keyboard-driven ('s' toggles add/remove; footer shows which).
        summary = self._summary_for(issue_key) if issue_key else None
        index = self._sprint_state.by_id or {}
        current = (
            index.get(summary.sprint_id)
            if summary is not None and summary.sprint_id is not None
            else None
        )
        name = (current.sprint_key or current.title or "-") if current else "-"
        return detail_row("Current Sprint", name)

    def _focused_in_active_sprint(self) -> bool:
        """Whether the focused issue already belongs to the active sprint (O(1))."""
        key = self._detail_state.issue_key
        if key is None:
            return False
        active = self._active_sprint()
        if active is None:
            return False
        summary = self._summary_for(key)
        return summary is not None and summary.sprint_id == active.id
