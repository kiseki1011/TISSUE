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
    type_chip as _type_chip,
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
    from tissue.api.generated.models.issue_detail_view import IssueDetailView
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary

log = logging.getLogger(__name__)


def _edit_button(button_id: str) -> TextButton:
    """A `✎` button (handled by EditsMixin) for an editable detail-row field."""
    return TextButton("✎", id=button_id, classes="hub-row-action hub-field-edit")


class DetailMixin(ProjectHomeBase):
    """The [2] Details pane.

    Fetch an issue, build the read view, and show it.
    """

    # Wait a moment before rendering a highlighted row
    # Holding ↓ scrolls past the rows in between without rendering each one
    _DETAIL_DEBOUNCE = 0.10

    def _cancel_detail_timer(self) -> None:
        if self._detail_timer is not None:
            self._detail_timer.stop()
            self._detail_timer = None

    def _debounce_detail(
        self, render: Callable[[], object], *, immediate: bool
    ) -> None:
        """Renders the [2] detail, but only after a short pause.

        Only renders the row you stop on.
        """
        self._cancel_detail_timer()
        if immediate:
            render()
        else:
            self._detail_timer = self.set_timer(self._DETAIL_DEBOUNCE, render)

    async def _render_issue_detail(
        self, issue_key: str, *, focus_detail: bool, force: bool = False
    ) -> None:
        """Draw the [2] detail, filling the pane as fast as the data allows.

        By case:
            - cached and not forced -> show it at once, then refresh in the
              background (an edit elsewhere is caught on the next look)
            - no cache -> a skeleton from the list row fills the pane instantly,
              then the real bundle replaces it
            - force (after an edit here) -> always refetch, skipping the cache
        """
        client = self.app.client
        if client is None:
            return
        # Re-show the timeline column, hidden for the timeline-less member view.
        self.remove_class("-no-timeline")
        self._detail_issue_key = issue_key
        # Always load activity on its own
        self.run_worker(
            self._load_activity(issue_key), exclusive=True, group="hub-activity"
        )

        cached = None if force else self._detail_cache.get(issue_key)
        if cached is not None:
            await self._apply_detail_view(cached, focus_detail=focus_detail)
            self.run_worker(
                self._revalidate_detail(issue_key),
                exclusive=True,
                group="hub-detail-revalidate",
            )
            return

        # No cache:
        # Skeleton from the list row we already have shows the structural fields with
        # zero wait, so the pane is never blank while the bundle is in flight.
        if not force:
            summary = self._summary_for(issue_key)
            if summary is not None:
                await self._mount_detail(self._skeleton_widgets(summary))

        try:
            view = await client.issues.get_issue_detail(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load issue %s: %s", issue_key, error)
            if self._detail_issue_key == issue_key:
                await self._mount_detail(
                    [Static("Couldn't load issue.", classes="hub-muted")]
                )
            return
        self._detail_cache[issue_key] = view
        await self._apply_detail_view(view, focus_detail=focus_detail)

    async def _revalidate_detail(self, issue_key: str) -> None:
        """Refetch a cached issue and redraw only when it actually changed.

        Lets a cache hit show instantly while a quiet refetch corrects anything
        an edit elsewhere has changed since.
        """
        client = self.app.client
        if client is None:
            return
        try:
            fresh = await client.issues.get_issue_detail(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to refresh issue %s: %s", issue_key, error)
            return
        if fresh == self._detail_cache.get(issue_key):
            return
        self._detail_cache[issue_key] = fresh
        await self._apply_detail_view(fresh, focus_detail=False)

    def _summary_for(self, issue_key: str) -> IssueSummary | None:
        """The already-loaded list row for `issue_key`, used to draw the skeleton."""
        for summary in (*self._issues, *self._agent_issues):
            if summary.issue_key == issue_key:
                return summary
        return None

    def _skeleton_widgets(self, summary: IssueSummary) -> list[Widget]:
        """The structural rows of a detail, built from the list row alone.

        Drawn instantly while the full bundle loads. The full view repeats these
        same rows, so the swap reads as the rest filling in, not a redraw.
        """
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._members
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

    async def _apply_detail_view(
        self, view: IssueDetailView, *, focus_detail: bool
    ) -> None:
        """Save the detail state and draw the full [2] read view from a bundle.

        Used for both a cached and a freshly fetched bundle.
        """
        issue = view.common
        if issue is None:
            await self._mount_detail(
                [Static("Couldn't load issue.", classes="hub-muted")]
            )
            return

        transitions = view.available_transitions or []
        custom_fields = view.custom_fields or []
        comments = (view.comments.content or []) if view.comments else []
        parent = view.parent
        children = view.children or []
        # The type list decides the +/✕ hierarchy controls. It's cached, so this
        # only fetches on the first issue viewed.
        await self._ensure_issue_type_hierarchy()
        # The sprint index resolves the current sprint
        await self._ensure_sprint_index()

        # The user may have moved to another issue while we awaited above, so
        # don't overwrite their pane (or its saved state) with a stale issue.
        if self._detail_issue_key != issue.issue_key:
            return

        self._detail_assigned = issue.assignee is not None
        # Each transition carries its own target state (no separate workflow call).
        target_labels = {
            transition.transition_id: (
                (
                    transition.target_state.display_name
                    if transition.target_state
                    else None
                )
                or "?"
            )
            for transition in transitions
            if transition.transition_id is not None
        }
        self._transitions_by_id = {
            transition.transition_id: transition
            for transition in transitions
            if transition.transition_id is not None
        }
        # Each custom field carries its own options (no separate issue-type call).
        options_by_field = {
            custom_field.field_id: list(custom_field.options or [])
            for custom_field in custom_fields
            if custom_field.field_id is not None
        }
        # EditsMixin reads these on a ✎ click to fill in the custom-field modal.
        self._detail_custom_fields = {
            custom_field.field_id: custom_field
            for custom_field in custom_fields
            if custom_field.field_id is not None
        }
        self._detail_field_options = options_by_field
        self._detail_hierarchy = (
            self._issue_type_hierarchy.get(issue.issue_type.id)
            if issue.issue_type and issue.issue_type.id is not None
            else None
        )
        self._detail_children = children
        self._detail_relations = view.relations

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
            log.exception("Hub: failed to build issue detail for %s", issue.issue_key)
            widgets = [Static("Couldn't render this issue.", classes="hub-muted")]
        await self._mount_detail(widgets)
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

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

    def _current_sprint_row(self, issue_key: str | None) -> Widget:
        """The issue's current sprint, with a `+` that adds it to the active sprint.

        The `+` is enabled only when the project has an active sprint.
        """
        summary = self._summary_for(issue_key) if issue_key else None
        index = self._sprints_by_id or {}
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
