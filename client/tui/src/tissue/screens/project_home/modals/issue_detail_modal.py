from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import ScrollableModal
from tissue.screens.project_home.modals.edit_issue_modal import EditIssueModal
from tissue.screens.project_home.modals.member_picker_modal import (
    UNASSIGN,
    MemberPickerModal,
)
from tissue.screens.project_home.modals.transition_picker_modal import (
    TransitionPickerModal,
)
from tissue.widgets.issue_link import IssueLink
from tissue.widgets.issue_read import issue_edit_current, issue_read_view

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

log = logging.getLogger(__name__)


class IssueDetailModal(ScrollableModal[None]):
    """Read an issue and act on it: edit, assign, transition, review."""

    CSS_PATH = "issue_detail_modal.tcss"

    BINDINGS = [
        Binding("e", "edit", "edit"),
        Binding("a", "assign", "assign"),
        Binding("t", "transition", "transition"),
        Binding("r", "review", "review"),
        Binding("escape", "close", "close"),
    ]

    def __init__(self, *, issue_key: str, project_key: str) -> None:
        super().__init__()
        self._issue_key = issue_key
        self._project_key = project_key
        self._detail: IssueCommonDetail | None = None
        self._custom_fields: list[CustomFieldValueInfo] = []
        self._options_by_field: dict[int, list[FieldOptionDetail]] = {}
        self._transitions: list[AvailableTransition] = []
        self._members: list[ProjectMemberSummary] | None = None

    def compose(self) -> ComposeResult:
        with Container(id="idm-dialog", classes="dialog"):
            with VerticalScroll(id="idm-scroll"):
                yield Vertical(Static("Loading…", classes="idm-muted"), id="idm-body")

    def on_mount(self) -> None:
        dialog = self.query_one("#idm-dialog", Container)
        dialog.border_title = self._issue_key
        dialog.border_subtitle = "Esc to close"
        self._reload()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(IssueLink.Clicked)
    def _on_hier_open(self, event: IssueLink.Clicked) -> None:
        """Open the linked issue. A click on this same issue is ignored."""
        event.stop()
        if event.issue_key != self._issue_key:
            self.app.push_screen(
                IssueDetailModal(
                    issue_key=event.issue_key, project_key=self._project_key
                )
            )

    def _reload(self) -> None:
        self.run_worker(self._load(), group="idm-load", exclusive=True)

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            view = await client.issues.get_issue_detail(self._issue_key)
        except TissueApiError as error:
            log.debug("Detail modal: failed to load %s: %s", self._issue_key, error)
            await self._mount([Static("Couldn't load issue.", classes="idm-muted")])
            return
        issue = view.common
        if issue is None:
            await self._mount([Static("Couldn't load issue.", classes="idm-muted")])
            return
        self._detail = issue
        self._custom_fields = view.custom_fields or []
        self._options_by_field = {
            custom_field.field_id: list(custom_field.options or [])
            for custom_field in self._custom_fields
            if custom_field.field_id is not None
        }
        self._transitions = view.available_transitions or []
        await self._mount(
            issue_read_view(
                issue,
                self._custom_fields,
                self._options_by_field,
                self.app.theme_variables,
                title_class="idm-title",
                content_class="idm-content",
                muted_class="idm-muted",
                show_reviewers=True,
                parent=view.parent,
                children=view.children or [],
                relations=view.relations,
            )
        )
        self.query_one("#idm-dialog", Container).border_subtitle = self._subtitle()

    def _subtitle(self) -> str:
        hints = ["e edit", "a assign"]
        if self._transitions:
            hints.append("t transition")
        if self._current_user_is_reviewer():
            hints.append("r review")
        hints.append("Esc close")
        return " · ".join(hints)

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#idm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)

    def action_edit(self) -> None:
        if self._detail is None:
            return
        self.app.push_screen(
            EditIssueModal(
                issue_key=self._issue_key,
                current=issue_edit_current(self._detail),
                custom_fields=list(self._custom_fields),
                options_by_field=self._options_by_field,
            ),
            self._on_mutated,
        )

    def action_assign(self) -> None:
        if self._detail is None:
            return
        self.run_worker(self._open_assign(), group="idm-action", exclusive=True)

    async def _open_assign(self) -> None:
        await self._ensure_members()
        if self._detail is None or self.app.screen is not self:
            return
        self.app.push_screen(
            MemberPickerModal(
                self._members or [], assigned=self._detail.assignee is not None
            ),
            self._on_member_picked,
        )

    def _on_member_picked(self, result: int | None) -> None:
        if result is None:
            return
        self.run_worker(
            self._apply_assignee(result), group="idm-action", exclusive=True
        )

    async def _apply_assignee(self, result: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if result == UNASSIGN:
                await client.issues.unassign_issue(self._issue_key)
            else:
                await client.issues.assign_issue(self._issue_key, result)
        except TissueApiError as error:
            log.debug("Detail modal: assign failed for %s: %s", self._issue_key, error)
            self.app.notify("Assign failed.", severity="error")
            return
        self._reload()

    def action_transition(self) -> None:
        if self._detail is None or not self._transitions:
            return
        current_label = (
            self._detail.current_state.display_name
            if self._detail.current_state
            else None
        ) or "-"
        target_labels = {
            transition.transition_id: (
                (
                    transition.target_state.display_name
                    if transition.target_state
                    else None
                )
                or "?"
            )
            for transition in self._transitions
            if transition.transition_id is not None
        }
        self.app.push_screen(
            TransitionPickerModal(self._transitions, current_label, target_labels),
            self._on_transition_picked,
        )

    def _on_transition_picked(self, transition_id: int | None) -> None:
        if transition_id is None:
            return
        self.run_worker(
            self._perform_transition(transition_id), group="idm-action", exclusive=True
        )

    async def _perform_transition(self, transition_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.perform_transition(self._issue_key, transition_id)
        except TissueApiError as error:
            log.debug(
                "Detail modal: transition failed for %s: %s", self._issue_key, error
            )
            self.app.notify("Transition failed.", severity="error")
            return
        self._reload()

    def action_review(self) -> None:
        if not self._current_user_is_reviewer():
            return
        from tissue.screens.project_home.modals.submit_review_modal import (
            SubmitReviewModal,
        )

        self.app.push_screen(
            SubmitReviewModal(issue_key=self._issue_key), self._on_review_decision
        )

    def _on_review_decision(self, approved: bool | None) -> None:
        if approved is None:
            return
        self.run_worker(
            self._submit_review(approved), group="idm-action", exclusive=True
        )

    async def _submit_review(self, approved: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.submit_review(self._issue_key, approved=approved)
        except TissueApiError as error:
            log.debug("Detail modal: review failed for %s: %s", self._issue_key, error)
            self.app.notify("Failed to submit review.", severity="error")
            return
        self.app.notify(
            "Review approved." if approved else "Changes requested.",
            severity="information",
        )
        self._reload()

    def _current_user_is_reviewer(self) -> bool:
        if self._detail is None:
            return False
        client = self.app.client
        profile = client.account.cached_profile if client else None
        username = profile.username if profile else None
        if not username:
            return False
        return any(
            reviewer.participant is not None
            and reviewer.participant.username == username
            for reviewer in (self._detail.reviewers or [])
        )

    def _on_mutated(self, updated: bool | None) -> None:
        if updated:
            self._reload()

    async def _ensure_members(self) -> None:
        if self._members is not None:
            return
        client = self.app.client
        if client is None:
            self._members = []
            return
        try:
            self._members = await client.project_members.list_project_members(
                self._project_key
            )
        except TissueApiError as error:
            log.debug("Detail modal: failed to load members: %s", error)
            self._members = []
