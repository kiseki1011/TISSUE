from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Button, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.widgets.issue_render import member_name, review_status_chip
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.reviewer_info import ReviewerInfo

log = logging.getLogger(__name__)

_REMOVE_PREFIX = "hub-reviewer-rm-"


class ReviewersMixin(ProjectHomeBase):
    """Reviewers section in issue detail."""

    def _reviewer_section(self, detail: IssueCommonDetail) -> list[Widget]:
        reviewers = detail.reviewers or []
        self._store_reviewer_state(detail, reviewers)
        widgets: list[Widget] = [
            Horizontal(
                Static("Reviewers", classes="hub-reviewer-title"),
                TextButton("+", id="hub-reviewer-add", classes="hub-row-action"),
                classes="hub-reviewer-header",
            )
        ]
        if not reviewers:
            widgets.append(Static("No reviewers.", classes="hub-muted"))
            return widgets
        for reviewer in reviewers:
            widgets.append(self._reviewer_row(reviewer))
        widgets.append(
            Horizontal(self._review_action(reviewers), classes="hub-request-row")
        )
        return widgets

    def _store_reviewer_state(
        self, detail: IssueCommonDetail, reviewers: list[ReviewerInfo]
    ) -> None:
        self._detail_assignee_id = (
            detail.assignee.member_id if detail.assignee else None
        )
        self._detail_reviewer_ids = [
            reviewer.participant.member_id
            for reviewer in reviewers
            if reviewer.participant and reviewer.participant.member_id is not None
        ]

    def _reviewer_row(self, reviewer: ReviewerInfo) -> Widget:
        participant = reviewer.participant
        member_id = participant.member_id if participant else None
        row: list[Widget] = [
            Static(member_name(participant), markup=False, classes="hub-reviewer-name"),
            Static(
                review_status_chip(self.app.theme_variables, reviewer.status),
                classes="hub-reviewer-status",
            ),
        ]
        if member_id is not None:
            row.append(self._remove_reviewer_button(member_id))
        return Horizontal(*row, classes="hub-reviewer-row")

    @staticmethod
    def _remove_reviewer_button(member_id: int) -> TextButton:
        return TextButton(
            "✕",
            id=f"{_REMOVE_PREFIX}{member_id}",
            classes="hub-row-action hub-reviewer-rm",
        )

    def _review_action(self, reviewers: list[ReviewerInfo]) -> TextButton:
        if self._current_user_is_reviewer(reviewers):
            return TextButton(
                "Submit review",
                id="hub-submit-review",
                classes="hub-request-btn",
            )
        return TextButton(
            "Request review",
            id="hub-request-review",
            classes="hub-request-btn",
        )

    def _current_user_is_reviewer(self, reviewers: list[ReviewerInfo]) -> bool:
        client = self.app.client
        profile = client.account.cached_profile if client else None
        username = profile.username if profile else None
        if not username:
            return False
        return any(
            reviewer.participant is not None
            and reviewer.participant.username == username
            for reviewer in reviewers
        )

    @on(Button.Pressed, "#hub-reviewer-add")
    def _on_reviewer_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._reviewer_busy:
            return
        self.run_worker(
            self._open_reviewer_picker(), exclusive=True, group="hub-reviewer"
        )

    async def _open_reviewer_picker(self) -> None:
        from tissue.screens.project_home.modals.reviewer_picker_modal import (
            ReviewerPickerModal,
        )

        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        await self._ensure_members_loaded()
        if self._detail_issue_key != issue_key:
            return
        self._reviewer_picker_issue = issue_key
        self._reviewer_picker_baseline = list(self._detail_reviewer_ids)
        self.app.push_screen(
            ReviewerPickerModal(
                members=self._members,
                current_reviewer_ids=self._reviewer_picker_baseline,
                assignee_id=self._detail_assignee_id,
            ),
            self._on_reviewers_picked,
        )

    def _on_reviewers_picked(self, picked: list[int] | None) -> None:
        if picked is None:
            return
        issue_key = self._reviewer_picker_issue
        if issue_key is None or self._detail_issue_key != issue_key:
            return
        if self._reviewer_busy:
            return
        current = set(self._reviewer_picker_baseline)
        chosen = set(picked)
        changes = (chosen - current, current - chosen)
        if not changes[0] and not changes[1]:
            return
        self._reviewer_busy = True
        self.run_worker(
            self._apply_reviewers(issue_key, *changes),
            exclusive=True,
            group="hub-reviewer-mut",
        )

    async def _apply_reviewers(
        self, issue_key: str, to_add: set[int], to_remove: set[int]
    ) -> None:
        client = self.app.client
        failed = 0
        try:
            if client is not None:
                failed += await self._remove_reviewers(client, issue_key, to_remove)
                failed += await self._add_reviewers(client, issue_key, to_add)
            if failed and self._detail_issue_key == issue_key:
                self.app.notify(
                    f"{failed} reviewer change(s) didn't apply.", severity="error"
                )
        finally:
            self._reviewer_busy = False
        self._refresh_detail(issue_key)

    async def _remove_reviewers(
        self, client, issue_key: str, member_ids: set[int]
    ) -> int:
        failed = 0
        for member_id in member_ids:
            try:
                await client.issues.remove_reviewer(issue_key, member_id)
            except TissueApiError as error:
                failed += 1
                log.debug("Hub: remove reviewer %s failed: %s", member_id, error)
        return failed

    async def _add_reviewers(self, client, issue_key: str, member_ids: set[int]) -> int:
        failed = 0
        for member_id in member_ids:
            try:
                await client.issues.add_reviewer(issue_key, member_id)
            except TissueApiError as error:
                failed += 1
                log.debug("Hub: add reviewer %s failed: %s", member_id, error)
        return failed

    @on(Button.Pressed, ".hub-reviewer-rm")
    def _on_reviewer_remove(self, event: Button.Pressed) -> None:
        event.stop()
        if self._reviewer_busy:
            return
        member_id = self._reviewer_id_from_button(event.button.id)
        if member_id is None:
            return
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        self._reviewer_busy = True
        self.run_worker(
            self._remove_reviewer(issue_key, member_id),
            exclusive=True,
            group="hub-reviewer-mut",
        )

    @staticmethod
    def _reviewer_id_from_button(button_id: str | None) -> int | None:
        if not button_id or not button_id.startswith(_REMOVE_PREFIX):
            return None
        try:
            return int(button_id[len(_REMOVE_PREFIX) :])
        except ValueError:
            return None

    async def _remove_reviewer(self, issue_key: str, member_id: int) -> None:
        client = self.app.client
        try:
            if client is not None:
                await client.issues.remove_reviewer(issue_key, member_id)
        except TissueApiError as error:
            log.debug("Hub: failed to remove reviewer from %s: %s", issue_key, error)
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to remove reviewer.", severity="error")
        finally:
            self._reviewer_busy = False
        self._refresh_detail(issue_key)

    @on(Button.Pressed, "#hub-request-review")
    def _on_request_review(self, event: Button.Pressed) -> None:
        event.stop()
        if self._reviewer_busy:
            return
        issue_key = self._detail_issue_key
        if issue_key is None or not self._detail_reviewer_ids:
            return
        self._reviewer_busy = True
        self.run_worker(
            self._request_review(issue_key, list(self._detail_reviewer_ids)),
            exclusive=True,
            group="hub-reviewer-mut",
        )

    async def _request_review(self, issue_key: str, member_ids: list[int]) -> None:
        client = self.app.client
        succeeded = False
        try:
            if client is not None:
                await client.issues.request_review(issue_key, member_ids)
                succeeded = True
        except TissueApiError as error:
            log.debug("Hub: failed to request review on %s: %s", issue_key, error)
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to request review.", severity="error")
        finally:
            self._reviewer_busy = False
        if succeeded and self._detail_issue_key == issue_key:
            self.app.notify("Review requested.", severity="information")
        self._refresh_detail(issue_key)

    @on(Button.Pressed, "#hub-submit-review")
    def _on_submit_review(self, event: Button.Pressed) -> None:
        event.stop()
        if self._reviewer_busy:
            return
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        from tissue.screens.project_home.modals.submit_review_modal import (
            SubmitReviewModal,
        )

        def on_decision(approved: bool | None) -> None:
            if approved is None:
                return
            if self._detail_issue_key != issue_key or self._reviewer_busy:
                return
            self._reviewer_busy = True
            self.run_worker(
                self._submit_review(issue_key, approved),
                exclusive=True,
                group="hub-reviewer-mut",
            )

        self.app.push_screen(SubmitReviewModal(issue_key=issue_key), on_decision)

    async def _submit_review(self, issue_key: str, approved: bool) -> None:
        client = self.app.client
        succeeded = False
        try:
            if client is not None:
                await client.issues.submit_review(issue_key, approved=approved)
                succeeded = True
        except TissueApiError as error:
            log.debug("Hub: failed to submit review on %s: %s", issue_key, error)
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to submit review.", severity="error")
        finally:
            self._reviewer_busy = False
        if succeeded and self._detail_issue_key == issue_key:
            self.app.notify(
                "Review approved." if approved else "Changes requested.",
                severity="information",
            )
        self._refresh_detail(issue_key)

    def _refresh_detail(self, issue_key: str) -> None:
        if self._detail_issue_key == issue_key:
            self.run_worker(
                self._render_issue_detail(issue_key, focus_detail=False, force=True),
                exclusive=True,
                group="hub-detail",
            )
