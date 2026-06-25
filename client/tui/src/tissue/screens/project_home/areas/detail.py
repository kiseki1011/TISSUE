from __future__ import annotations

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
    """The [2] Details pane: fetch an issue + its transitions, build the read
    view (fields, content, comments), and mount it into the scrollable body."""

    # How long the cursor must rest on a list row before its detail renders. Short
    # enough to feel instant on a deliberate move, long enough that holding ↓ scrolls
    # past intermediate rows without rendering (and re-fetching) each one.
    _DETAIL_DEBOUNCE = 0.12

    def _cancel_detail_timer(self) -> None:
        if self._detail_timer is not None:
            self._detail_timer.stop()
            self._detail_timer = None

    def _debounce_detail(
        self, render: Callable[[], object], *, immediate: bool
    ) -> None:
        """Render the [2] detail, debounced. A transient highlight (cursor moving
        through the list) schedules `render` after a short settle, so rapid
        navigation only renders the row it lands on — every list view shares this so
        the detail-render worker group can't be flooded with cancel-on-arrival
        renders. An explicit selection (Enter) renders immediately. Any pending
        timer is always cleared first, so only one render is ever queued."""
        self._cancel_detail_timer()
        if immediate:
            render()
        else:
            self._detail_timer = self.set_timer(self._DETAIL_DEBOUNCE, render)

    async def _render_issue_detail(self, issue_key: str, *, focus_detail: bool) -> None:
        client = self.app.client
        if client is None:
            return
        # Issues have an activity timeline — make sure the column (hidden for the
        # timeline-less member view) is visible again.
        self.remove_class("-no-timeline")
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
        options_by_field = await self._load_field_options(issue, custom_fields)
        # Stash for the custom-field edit modal (EditsMixin reads these on a ✎ click).
        self._detail_custom_fields = {
            cf.field_id: cf for cf in custom_fields if cf.field_id is not None
        }
        self._detail_field_options = options_by_field
        # Load comments up front and mount them with the rest of the detail, so the
        # comments appear in the same paint — not as a "Loading…" placeholder that
        # pops into the real thread a moment later (the flicker on issue switch).
        try:
            comments = await client.comments.list_issue_comments(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load comments for %s: %s", issue_key, e)
            comments = []
        # Parent/children aren't on the detail DTO — fetch them separately (and the
        # type catalog, to resolve this issue's hierarchy level, which gates the
        # +/✕ controls). All best-effort: the section degrades to empty on failure.
        await self._ensure_issue_type_hierarchy()
        self._detail_hierarchy = (
            self._issue_type_hierarchy.get(issue.issue_type.id)
            if issue.issue_type and issue.issue_type.id is not None
            else None
        )
        try:
            parent = await client.issues.get_issue_parent(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load parent for %s: %s", issue_key, e)
            parent = None
        try:
            children = await client.issues.get_issue_children(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load children for %s: %s", issue_key, e)
            children = []
        self._detail_children = children
        try:
            self._detail_relations = await client.issues.get_issue_relations(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load relations for %s: %s", issue_key, e)
            self._detail_relations = None
        # Building the read view is pure-Python over server data; guard it broadly
        # so a single issue with a shape we didn't anticipate degrades to a "couldn't
        # render" note instead of an unhandled exception that takes the whole app
        # down (the fetches above are already TissueApiError-guarded individually).
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

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """The issue type's field options (field id -> options), so SELECT_OPTION /
        CHECKLIST fields resolve ids to names and the edit modal can offer the
        choices. Fetched only when an option-bearing field is present; best-effort."""
        needs_options = any(
            cf.issue_field_type in ("SELECT_OPTION", "CHECKLIST")
            for cf in custom_fields
        )
        client = self.app.client
        if not needs_options or client is None:
            return {}
        type_id = issue.issue_type.id if issue.issue_type else None
        if type_id is None:
            return {}
        try:
            issue_type = await client.issues.get_issue_type(type_id)
        except TissueApiError as e:
            log.debug("Hub: failed to load issue type %s options: %s", type_id, e)
            return {}
        return {
            f.id: list(f.options or [])
            for f in (issue_type.fields or [])
            if f.id is not None
        }

    def _cf_edit_button(self, field_id: int) -> TextButton:
        """A ✎ button for a custom-field row (handled by EditsMixin)."""
        return TextButton(
            "✎", id=f"hub-cf-edit-{field_id}", classes="hub-row-action hub-cf-edit"
        )

    def _issue_widgets(
        self,
        d: IssueCommonDetail,
        transitions: list[AvailableTransition],
        target_labels: dict[int, str],
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        comments: list[CommentDetailResponse],
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
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
            # The Markdown body, prefilled into the description edit modal.
            "content": d.content or "",
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
            *progress_block(d),
            detail_row(
                "Due",
                format_relative(d.due_at),
                action=_edit_button("hub-edit-due"),
            ),
            detail_row("Created", format_relative(d.created_at)),
            detail_row("Updated", format_relative(d.last_updated_at)),
            # The issue type's custom fields: a blank line below the standard
            # fields, each with a ✎ to edit it (type-specific modal).
            *custom_field_section(
                custom_fields, options_by_field, edit_button=self._cf_edit_button
            ),
            # Reviewers occupy the custom-field slot: after the last custom field
            # (or after Updated when there are none), one blank line down.
            *self._reviewer_section(d),
            # Parent / children, mirroring the reviewers section's +/✕ controls.
            *self._hierarchy_section(d, parent, children),
            # Issue relations (blocks/causes/duplicates/relevant), below the hierarchy.
            *self._relations_section(d),
            Rule(),
            # A bare ✎ at the description's top-right (no "Description" label),
            # aligned with the field-edit pencils above; opens the editor modal.
            Horizontal(
                TextButton(
                    "✎",
                    id="hub-edit-description",
                    classes="hub-row-action hub-desc-edit",
                ),
                classes="hub-desc-header",
            ),
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
        try:
            inner = self.query_one("#hub-detail-main-inner")
        except NoMatches:
            # The pane is gone (screen tearing down / mid-recompose) — skip rather
            # than let a NoMatches escape the worker and crash the whole app.
            return
        # The action-row controls carry fixed ids, so the old set must be gone
        # before the new mounts (else DuplicateIds) — await the removal. Batch the
        # clear + remount so the pane repaints once, not as an empty frame then a
        # full one (the flicker when switching issues).
        with self.app.batch_update():
            await inner.remove_children()
            await inner.mount(*widgets)

    async def _reset_detail_pane(self) -> None:
        """Clear [2] back to its empty placeholder. Used when the [1] list becomes
        empty (e.g. a filter matches nothing) so the detail doesn't keep showing an
        issue that's no longer in the list. Drops the issue key so any late
        comment/activity workers bail, and clears the timeline."""
        self._detail_issue_key = None
        self.remove_class("-no-timeline")
        await self._mount_detail(
            [Static("Select an issue to see details.", classes="hub-muted")]
        )
        await self._clear_timeline()

    def action_focus_detail(self) -> None:
        self.query_one("#hub-detail-main").focus()
