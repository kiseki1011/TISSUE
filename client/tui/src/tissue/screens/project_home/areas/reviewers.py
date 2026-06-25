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
    """The issue detail's reviewers: a list of reviewers with their status, a '+'
    to edit the set (multi-select picker), a ✕ to drop one, and a 'Request review'
    button that pings the current reviewers. Sits in the custom-field slot (after
    the custom fields, or after Updated when there are none)."""

    def _reviewer_section(self, d: IssueCommonDetail) -> list[Widget]:
        reviewers = d.reviewers or []
        # Stash for the picker (diff) and request-review (who to ping).
        self._detail_assignee_id = d.assignee.member_id if d.assignee else None
        self._detail_reviewer_ids = [
            r.participant.member_id
            for r in reviewers
            if r.participant and r.participant.member_id is not None
        ]
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
        for r in reviewers:
            p = r.participant
            mid = p.member_id if p else None
            row: list[Widget] = [
                Static(member_name(p), markup=False, classes="hub-reviewer-name"),
                Static(
                    review_status_chip(self.app.theme_variables, r.status),
                    classes="hub-reviewer-status",
                ),
            ]
            if mid is not None:
                row.append(
                    TextButton(
                        "✕",
                        id=f"{_REMOVE_PREFIX}{mid}",
                        classes="hub-row-action hub-reviewer-rm",
                    )
                )
            widgets.append(Horizontal(*row, classes="hub-reviewer-row"))
        # The right-aligned action button. If the current user is one of the
        # reviewers, they submit their own decision (Approve / Request changes);
        # otherwise the button pings the reviewers to review. Reviewer identity is
        # matched by username — the only current-user handle available client-side
        # (MemberProfile carries no member id).
        if self._current_user_is_reviewer(reviewers):
            action = Button(
                "Submit review",
                id="hub-submit-review",
                classes="-btn-success hub-request-btn",
            )
        else:
            action = Button(
                "Request review",
                id="hub-request-review",
                classes="-btn-success hub-request-btn",
            )
        widgets.append(Horizontal(action, classes="hub-request-row"))
        return widgets

    def _current_user_is_reviewer(self, reviewers: list[ReviewerInfo]) -> bool:
        """Whether the logged-in user is among the issue's reviewers, matched by
        username (the cached profile has no member id to compare against the
        numeric reviewer ids)."""
        client = self.app.client
        profile = client.account.cached_profile if client else None
        username = profile.username if profile else None
        if not username:
            return False
        return any(
            r.participant is not None and r.participant.username == username
            for r in reviewers
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
            return  # the detail moved on while members loaded
        # Snapshot what the picker edits, so its result diffs against THIS baseline
        # even if the detail re-renders or switches issue while the picker is up.
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
        # Only apply if the detail is still on the issue the picker was opened for.
        if issue_key is None or self._detail_issue_key != issue_key:
            return
        if self._reviewer_busy:
            return
        current = set(self._reviewer_picker_baseline)
        chosen = set(picked)
        to_add = chosen - current
        to_remove = current - chosen
        if not to_add and not to_remove:
            return
        self._reviewer_busy = True
        self.run_worker(
            self._apply_reviewers(issue_key, to_add, to_remove),
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
                # Isolate each call: one failure (a network blip, or a 10-cap
                # rejection on an add) must not abort the rest of the change.
                for mid in to_remove:
                    try:
                        await client.issues.remove_reviewer(issue_key, mid)
                    except TissueApiError as e:
                        failed += 1
                        log.debug("Hub: remove reviewer %s failed: %s", mid, e)
                for mid in to_add:
                    try:
                        await client.issues.add_reviewer(issue_key, mid)
                    except TissueApiError as e:
                        failed += 1
                        log.debug("Hub: add reviewer %s failed: %s", mid, e)
            if failed and self._detail_issue_key == issue_key:
                self.app.notify(
                    f"{failed} reviewer change(s) didn't apply.", severity="error"
                )
        finally:
            self._reviewer_busy = False
        self._refresh_detail(issue_key)

    @on(Button.Pressed, ".hub-reviewer-rm")
    def _on_reviewer_remove(self, event: Button.Pressed) -> None:
        event.stop()
        if self._reviewer_busy:
            return
        bid = event.button.id or ""
        if not bid.startswith(_REMOVE_PREFIX):
            return
        try:
            mid = int(bid[len(_REMOVE_PREFIX) :])
        except ValueError:
            return
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        self._reviewer_busy = True
        self.run_worker(
            self._remove_reviewer(issue_key, mid),
            exclusive=True,
            group="hub-reviewer-mut",
        )

    async def _remove_reviewer(self, issue_key: str, member_id: int) -> None:
        client = self.app.client
        try:
            if client is not None:
                await client.issues.remove_reviewer(issue_key, member_id)
        except TissueApiError as e:
            log.debug("Hub: failed to remove reviewer from %s: %s", issue_key, e)
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
        ok = False
        try:
            if client is not None:
                await client.issues.request_review(issue_key, member_ids)
                ok = True
        except TissueApiError as e:
            log.debug("Hub: failed to request review on %s: %s", issue_key, e)
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to request review.", severity="error")
        finally:
            self._reviewer_busy = False
        if ok and self._detail_issue_key == issue_key:
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
                return  # cancelled
            # The detail may have moved on (or a mutation started) while the modal
            # was up — re-check before submitting.
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
        ok = False
        try:
            if client is not None:
                await client.issues.submit_review(issue_key, approved=approved)
                ok = True
        except TissueApiError as e:
            log.debug("Hub: failed to submit review on %s: %s", issue_key, e)
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to submit review.", severity="error")
        finally:
            self._reviewer_busy = False
        # Only surface the outcome if the detail is still on this issue — the worker
        # runs in its own group (uncancelled by a detail switch), so a toast for an
        # issue the user already navigated away from would otherwise be misleading.
        if ok and self._detail_issue_key == issue_key:
            self.app.notify(
                "Review approved." if approved else "Changes requested.",
                severity="information",
            )
        self._refresh_detail(issue_key)

    def _refresh_detail(self, issue_key: str) -> None:
        """Re-render [2] if it's still on `issue_key`, scheduled in the shared detail
        group so it serialises with every other render (no concurrent remove/mount).
        Skipped when the worker was cancelled mid-flight (detail no longer here)."""
        if self._detail_issue_key == issue_key:
            self.run_worker(
                self._render_issue_detail(issue_key, focus_detail=False),
                exclusive=True,
                group="hub-detail",
            )
