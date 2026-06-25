from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.css.query import NoMatches
from textual.widgets import Button, Input, Label, SelectionList
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

# Mirrors backend policy tissue.issue.policy.max-reviewers.
MAX_REVIEWERS = 10


class ReviewerPickerModal(TissueModal["list[int] | None"]):
    """Pick the issue's reviewers, multi-select, with the current ones ticked.

    `_checked` remembers the picks so they stay when the list is rebuilt on
    search. Dismisses with the chosen reviewer ids, or None on cancel.
    """

    CSS_PATH = "reviewer_picker_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(
        self,
        *,
        members: list[ProjectMemberSummary],
        current_reviewer_ids: list[int],
        assignee_id: int | None,
    ) -> None:
        super().__init__()
        # Exclude the assignee, backend forbids assignee == reviewer.
        self._candidates: list[tuple[str, int]] = []
        for member in members:
            if member.member_id is None or member.member_id == assignee_id:
                continue
            self._candidates.append(
                (member.display_name or member.username or "-", member.member_id)
            )
        self._checked: set[int] = {
            member_id for member_id in current_reviewer_ids if member_id != assignee_id
        }
        self._search_text = ""
        # Ignore SelectedChanged while we rebuild the list ourselves.
        self._rebuilding = False

    def _selections(self) -> list[Selection[int]]:
        search_text = self._search_text
        return [
            Selection(label, value, value in self._checked)
            for label, value in self._candidates
            if not search_text or search_text in label.casefold()
        ]

    def compose(self) -> ComposeResult:
        with Container(id="reviewer-dialog", classes="dialog"):
            yield Input(placeholder="Search members…", id="reviewer-search")
            yield SelectionList[int](*self._selections(), id="reviewer-list")
            yield Label("", id="reviewer-count", classes="reviewer-count")
            with Horizontal(id="reviewer-buttons"):
                yield Button("Cancel", id="reviewer-cancel", classes="-btn-error")
                yield Button("Apply", id="reviewer-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#reviewer-dialog", Container)
        dialog.border_title = "Reviewers"
        dialog.border_subtitle = "Esc to cancel"
        self._update_count()
        try:
            self.query_one("#reviewer-list", SelectionList).focus()
        except NoMatches:
            pass

    @on(Input.Changed, "#reviewer-search")
    def _on_search(self, event: Input.Changed) -> None:
        self._sync_checked()
        self._search_text = event.value.strip().casefold()
        self._rebuild()

    @on(SelectionList.SelectedChanged, "#reviewer-list")
    def _on_changed(self) -> None:
        if self._rebuilding:
            return
        self._sync_checked()
        self._update_count()

    def _sync_checked(self) -> None:
        """Add the on-screen ticks into `_checked` so hidden picks stay."""
        try:
            selection_list = self.query_one("#reviewer-list", SelectionList)
        except NoMatches:
            return
        shown = {
            value
            for label, value in self._candidates
            if not self._search_text or self._search_text in label.casefold()
        }
        self._checked = (self._checked - shown) | set(selection_list.selected)

    def _rebuild(self) -> None:
        try:
            selection_list = self.query_one("#reviewer-list", SelectionList)
        except NoMatches:
            return
        self._rebuilding = True
        try:
            selection_list.clear_options()
            selection_list.add_options(self._selections())
        finally:
            self._rebuilding = False

    def _update_count(self) -> None:
        try:
            label = self.query_one("#reviewer-count", Label)
        except NoMatches:
            return
        checked_count = len(self._checked)
        label.update(f"{checked_count} / {MAX_REVIEWERS} selected")
        label.set_class(checked_count > MAX_REVIEWERS, "-over-limit")

    @on(Button.Pressed, "#reviewer-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        self._sync_checked()
        if len(self._checked) > MAX_REVIEWERS:
            self._update_count()
            self.app.notify(
                f"At most {MAX_REVIEWERS} reviewers — deselect a few.",
                severity="warning",
            )
            return
        self.dismiss(sorted(self._checked))

    @on(Button.Pressed, "#reviewer-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)
