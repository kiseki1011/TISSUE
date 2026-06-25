from __future__ import annotations

import asyncio
import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Horizontal
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_render import (
    color_chip as _color_chip,
)
from tissue.widgets.issue_render import (
    custom_field_section,
    progress_block,
)
from tissue.widgets.issue_render import (
    member_name as _member_name,
)
from tissue.widgets.issue_render import (
    priority_chip as _priority_chip,
)
from tissue.widgets.issue_render import (
    type_text as _type_text,
)
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from collections.abc import Callable

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

log = logging.getLogger(__name__)


def _edit_button(button_id: str) -> TextButton:
    """A `✎` button (handled by EditsMixin) for an editable detail-row field."""
    return TextButton("✎", id=button_id, classes="hub-row-action hub-field-edit")


class DetailMixin(ProjectHomeBase):
    """The [2] Details pane.

    Fetch an issue, build the read view, and show it.
    """

    # Wait a moment before drawing a highlighted row, so holding ↓ scrolls past
    # the rows in between without drawing (and re-fetching) each one.
    _DETAIL_DEBOUNCE = 0.12

    def _cancel_detail_timer(self) -> None:
        if self._detail_timer is not None:
            self._detail_timer.stop()
            self._detail_timer = None

    def _debounce_detail(
        self, render: Callable[[], object], *, immediate: bool
    ) -> None:
        """Draw the [2] detail, but only after a short pause.

        A passing highlight settles first, so moving fast only draws the row you
        stop on. Picking a row on purpose (immediate) draws it right away.
        """
        self._cancel_detail_timer()
        if immediate:
            render()
        else:
            self._detail_timer = self.set_timer(self._DETAIL_DEBOUNCE, render)

    async def _render_issue_detail(self, issue_key: str, *, focus_detail: bool) -> None:
        client = self.app.client
        if client is None:
            return
        # Re-show the timeline column, hidden for the timeline-less member view.
        self.remove_class("-no-timeline")
        self._detail_issue_key = issue_key
        try:
            issue = await client.issues.get_issue(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load issue %s: %s", issue_key, error)
            await self._mount_detail(
                [Static("Couldn't load issue.", classes="hub-muted")]
            )
            return
        self._detail_assigned = issue.assignee is not None
        try:
            transitions = await client.issues.get_transitions(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load transitions for %s: %s", issue_key, error)
            transitions = []
        target_labels = await self._target_state_labels(transitions)
        self._transitions_by_id = {
            transition.transition_id: transition
            for transition in transitions
            if transition.transition_id is not None
        }
        try:
            custom_fields = await client.issues.get_issue_custom_fields(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load custom fields for %s: %s", issue_key, error)
            custom_fields = []
        options_by_field = await self._load_field_options(issue, custom_fields)
        # EditsMixin reads these on a ✎ click to fill in the custom-field modal.
        self._detail_custom_fields = {
            custom_field.field_id: custom_field
            for custom_field in custom_fields
            if custom_field.field_id is not None
        }
        self._detail_field_options = options_by_field
        # Show comments at the same time as the rest, so switching issue doesn't
        # flash a "Loading…" first.
        try:
            comments = await client.comments.list_issue_comments(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load comments for %s: %s", issue_key, error)
            comments = []
        # Parent/children aren't in the issue data, so fetch them separately. The
        # type list tells us the hierarchy level that decides the +/✕ controls.
        # If any of this fails we just show an empty section.
        await self._ensure_issue_type_hierarchy()
        self._detail_hierarchy = (
            self._issue_type_hierarchy.get(issue.issue_type.id)
            if issue.issue_type and issue.issue_type.id is not None
            else None
        )
        try:
            parent = await client.issues.get_issue_parent(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load parent for %s: %s", issue_key, error)
            parent = None
        try:
            children = await client.issues.get_issue_children(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load children for %s: %s", issue_key, error)
            children = []
        self._detail_children = children
        try:
            self._detail_relations = await client.issues.get_issue_relations(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load relations for %s: %s", issue_key, error)
            self._detail_relations = None
        # An unexpected issue shape shows a "couldn't render" note instead of
        # taking the whole app down.
        widgets: list[Widget]
        try:
            widgets = self._issue_widgets(
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
            log.exception("Hub: failed to build issue detail for %s", issue_key)
            widgets = [Static("Couldn't render this issue.", classes="hub-muted")]
        await self._mount_detail(widgets)
        self.run_worker(
            self._load_activity(issue_key), exclusive=True, group="hub-activity"
        )
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    async def _target_state_labels(
        self, transitions: list[AvailableTransition]
    ) -> dict[int, str]:
        """Map each transition id to the label of the state it leads to.

        Looked up in the saved workflow because the available-transitions
        response leaves out the target state.
        """
        client = self.app.client
        workflow_id = next(
            (
                transition.workflow_id
                for transition in transitions
                if transition.workflow_id is not None
            ),
            None,
        )
        if client is None or workflow_id is None:
            return {}
        workflow = self._workflow_cache.get(workflow_id)
        if workflow is None:
            try:
                workflow = await client.workflows.get_workflow(workflow_id)
            except TissueApiError as error:
                log.debug("Hub: failed to load workflow %s: %s", workflow_id, error)
                return {}
            self._workflow_cache[workflow_id] = workflow
        state_label = {
            state.id: state.label
            for state in (workflow.states or [])
            if state.id is not None
        }
        return {
            workflow_transition.id: state_label.get(workflow_transition.target_state_id)
            or "?"
            for workflow_transition in (workflow.transitions or [])
            if workflow_transition.id is not None
            and workflow_transition.target_state_id is not None
        }

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """The issue type's field options, keyed by field id.

        Lets SELECT_OPTION / CHECKLIST fields show names instead of ids and gives
        the edit modal its choices. Fetched only when a field with options is
        present. If it fails we just skip it.
        """
        needs_options = any(
            custom_field.issue_field_type in ("SELECT_OPTION", "CHECKLIST")
            for custom_field in custom_fields
        )
        client = self.app.client
        if not needs_options or client is None:
            return {}
        type_id = issue.issue_type.id if issue.issue_type else None
        if type_id is None:
            return {}
        try:
            issue_type = await client.issues.get_issue_type(type_id)
        except TissueApiError as error:
            log.debug("Hub: failed to load issue type %s options: %s", type_id, error)
            return {}
        return {
            field.id: list(field.options or [])
            for field in (issue_type.fields or [])
            if field.id is not None
        }

    def _cf_edit_button(self, field_id: int) -> TextButton:
        """A ✎ button for a custom-field row (handled by EditsMixin)."""
        return TextButton(
            "✎", id=f"hub-cf-edit-{field_id}", classes="hub-row-action hub-cf-edit"
        )

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
        # A field-edit (✎) modal fills in these current values.
        self._edit_current = {
            "title": detail.title or "",
            "priority": detail.priority or "",
            # Full ISO instant (not a date) so the DateTimePicker shows the time too.
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
            # Reviewers take the custom-field slot, one blank line below the last
            # custom field, or below Updated when there are none.
            *self._reviewer_section(detail),
            *self._hierarchy_section(detail, parent, children),
            *self._relations_section(detail),
            Rule(),
            # Plain ✎ at the description's top-right, lined up with the field-edit
            # pencils above.
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

    async def _mount_detail(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#hub-detail-main-inner")
        except NoMatches:
            # Pane is gone (being torn down or rebuilt), so don't let NoMatches
            # escape the worker and crash the app.
            return

        async def swap() -> None:
            # Wait for the old widgets to go before adding new ones, or controls
            # with the same id clash (DuplicateIds). Group the change so the pane
            # repaints once, not empty-then-full. The lock keeps two fast renders
            # from interleaving their swaps.
            async with self._detail_mount_lock:
                with self.app.batch_update():
                    await inner.remove_children()
                    await inner.mount(*widgets)

        # Shield so a render worker cancelled mid-swap (fast navigation) still
        # finishes it. A half-done swap leaves the pane blank, and the cancelled
        # removal leaks a CancelledError that quietly shuts the whole app down.
        await asyncio.shield(swap())

    async def _reset_detail_pane(self) -> None:
        """Clear [2] back to its placeholder when the [1] list is empty.

        Forgets the issue key so late comment/activity workers stop early.
        """
        self._detail_issue_key = None
        self.remove_class("-no-timeline")
        await self._mount_detail(
            [Static("Select an issue to see details.", classes="hub-muted")]
        )
        await self._clear_timeline()

    def action_focus_detail(self) -> None:
        self.query_one("#hub-detail-main").focus()
