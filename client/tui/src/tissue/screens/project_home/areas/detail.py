from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.rendering import (
    _color_chip,
    _custom_field_label,
    _custom_field_value,
    _member_name,
    _priority_chip,
    _type_text,
)
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)


def _edit_button(button_id: str) -> TextButton:
    """A `✎` button (handled by EditsMixin) for an editable detail-row field."""
    return TextButton("✎", id=button_id, classes="hub-row-action hub-field-edit")


class DetailMixin(ProjectHomeBase):
    """The [2] Details pane: fetch an issue + its transitions, build the read
    view (fields, content, comments), and mount it into the scrollable body."""

    async def _render_issue_detail(self, issue_key: str, *, focus_detail: bool) -> None:
        client = self.app.client
        if client is None:
            return
        self._detail_issue_key = issue_key
        try:
            issue = await client.issues.get_issue(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load issue %s: %s", issue_key, e)
            await self._mount_detail(
                [Static("Couldn't load issue.", classes="hub-muted")]
            )
            return
        self._detail_assigned = issue.assignee is not None
        try:
            transitions = await client.issues.get_transitions(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load transitions for %s: %s", issue_key, e)
            transitions = []
        target_labels = await self._target_state_labels(transitions)
        self._transitions_by_id = {
            t.transition_id: t for t in transitions if t.transition_id is not None
        }
        try:
            custom_fields = await client.issues.get_issue_custom_fields(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load custom fields for %s: %s", issue_key, e)
            custom_fields = []
        # Load comments up front and mount them with the rest of the detail, so the
        # comments appear in the same paint — not as a "Loading…" placeholder that
        # pops into the real thread a moment later (the flicker on issue switch).
        try:
            comments = await client.comments.list_issue_comments(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load comments for %s: %s", issue_key, e)
            comments = []
        await self._mount_detail(
            self._issue_widgets(
                issue, transitions, target_labels, custom_fields, comments
            )
        )
        self.run_worker(
            self._load_activity(issue_key), exclusive=True, group="hub-activity"
        )
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    async def _target_state_labels(
        self, transitions: list[AvailableTransition]
    ) -> dict[int, str]:
        """Map each transition id to its target state's label via the workflow
        graph (cached). The available-transitions response doesn't carry the
        target state, so it's looked up in the workflow."""
        client = self.app.client
        workflow_id = next(
            (t.workflow_id for t in transitions if t.workflow_id is not None), None
        )
        if client is None or workflow_id is None:
            return {}
        workflow = self._workflow_cache.get(workflow_id)
        if workflow is None:
            try:
                workflow = await client.workflows.get_workflow(workflow_id)
            except TissueApiError as e:
                log.debug("Hub: failed to load workflow %s: %s", workflow_id, e)
                return {}
            self._workflow_cache[workflow_id] = workflow
        state_label = {
            s.id: s.label for s in (workflow.states or []) if s.id is not None
        }
        return {
            wt.id: state_label.get(wt.target_state_id) or "?"
            for wt in (workflow.transitions or [])
            if wt.id is not None and wt.target_state_id is not None
        }

    def _issue_widgets(
        self,
        d: IssueCommonDetail,
        transitions: list[AvailableTransition],
        target_labels: dict[int, str],
        custom_fields: list[CustomFieldValueInfo],
        comments: list[CommentDetailResponse],
    ) -> list[Widget]:
        state = d.current_state
        issue_type = d.issue_type
        current_state_label = (state.display_name if state else None) or "-"
        # Stash current values so a field-edit (✎) modal can prefill them.
        self._edit_current = {
            "title": d.title or "",
            "priority": d.priority or "",
            # Full ISO instant (not a date) so the edit modal's DateTimePicker can
            # prefill the time too.
            "dueAt": d.due_at.isoformat() if d.due_at else "",
            "storyPoint": "" if d.story_point is None else str(d.story_point),
        }
        widgets: list[Widget] = [
            Horizontal(
                Static(d.title or "-", markup=False, classes="hub-detail-title"),
                _edit_button("hub-edit-title"),
                classes="hub-title-row",
            ),
            detail_row("Key", d.issue_key or "-"),
            detail_row(
                "Status",
                _color_chip(current_state_label, state.color if state else None),
                action=self._status_action(
                    transitions, current_state_label, target_labels
                ),
            ),
            detail_row(
                "Priority",
                _priority_chip(self.app.theme_variables, d.priority),
                action=_edit_button("hub-edit-priority"),
            ),
            detail_row("Type", _type_text(issue_type)),
            detail_row(
                "Assignee",
                _member_name(d.assignee),
                action=TextButton(
                    "✎", id="hub-assignee-edit", classes="hub-row-action"
                ),
            ),
            detail_row("Author", _member_name(d.author)),
            detail_row(
                "Story points",
                "-" if d.story_point is None else str(d.story_point),
                action=_edit_button("hub-edit-sp"),
            ),
            # The issue type's custom fields (read-only), between Story points
            # and Due.
            *[
                detail_row(_custom_field_label(cf), _custom_field_value(cf))
                for cf in custom_fields
            ],
            detail_row(
                "Due",
                format_relative(d.due_at),
                action=_edit_button("hub-edit-due"),
            ),
            detail_row("Created", format_relative(d.created_at)),
            detail_row("Updated", format_relative(d.last_updated_at)),
            Rule(),
        ]
        content = (d.content or "").strip()
        widgets.append(
            Markdown(content, classes="hub-content")
            if content
            else Static(Text("(empty)", style="italic"), classes="hub-muted")
        )
        widgets.extend(self._comment_section(comments))
        return widgets

    async def _mount_detail(self, widgets: list[Widget]) -> None:
        inner = self.query_one("#hub-detail-main-inner")
        # The action-row controls carry fixed ids, so the old set must be gone
        # before the new mounts (else DuplicateIds) — await the removal. Batch the
        # clear + remount so the pane repaints once, not as an empty frame then a
        # full one (the flicker when switching issues).
        with self.app.batch_update():
            await inner.remove_children()
            await inner.mount(*widgets)

    def action_focus_detail(self) -> None:
        self.query_one("#hub-detail-main").focus()
