"""Multi-select reviewer picker for an issue (opened by the Reviewers '+' button).

Mirrors the filter modal's assignee block: a client-side search box over a
SelectionList, with the checked set tracked in `_checked` so it survives the
list being rebuilt as you search. The current assignee is excluded (the backend
forbids assignee == reviewer) and the selection is capped at 10. Dismisses with
the chosen member-id list, or None on cancel; the caller diffs it against the
issue's current reviewers to add/remove.
"""

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

# Backend policy: at most 10 reviewers per issue (tissue.issue.policy.max-reviewers).
MAX_REVIEWERS = 10


class ReviewerPickerModal(TissueModal["list[int] | None"]):
    """Pick the issue's reviewers (multi-select, OR). Pre-checks the current set."""

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
        # Candidates: every project member except the current assignee (who cannot
        # also be a reviewer). label -> member id.
        self._all: list[tuple[str, int]] = []
        for m in members:
            if m.member_id is None or m.member_id == assignee_id:
                continue
            self._all.append((m.display_name or m.username or "-", m.member_id))
        self._checked: set[int] = {
            mid for mid in current_reviewer_ids if mid != assignee_id
        }
        self._kw = ""
        # Guards SelectedChanged while we programmatically rebuild the list.
        self._rebuilding = False

    def _selections(self) -> list[Selection[int]]:
        kw = self._kw
        return [
            Selection(label, value, value in self._checked)
            for label, value in self._all
            if not kw or kw in label.casefold()
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
        self._kw = event.value.strip().casefold()
        self._rebuild()

    @on(SelectionList.SelectedChanged, "#reviewer-list")
    def _on_changed(self) -> None:
        if self._rebuilding:
            return
        self._sync_checked()
        self._update_count()

    def _sync_checked(self) -> None:
        """Fold the live selection into the tracked set: drop the currently-shown
        values, then re-add the checked ones — so filtered-out picks survive."""
        try:
            select = self.query_one("#reviewer-list", SelectionList)
        except NoMatches:
            return
        shown = {
            value
            for label, value in self._all
            if not self._kw or self._kw in label.casefold()
        }
        self._checked = (self._checked - shown) | set(select.selected)

    def _rebuild(self) -> None:
        try:
            select = self.query_one("#reviewer-list", SelectionList)
        except NoMatches:
            return
        self._rebuilding = True
        try:
            select.clear_options()
            select.add_options(self._selections())
        finally:
            self._rebuilding = False

    def _update_count(self) -> None:
        try:
            label = self.query_one("#reviewer-count", Label)
        except NoMatches:
            return
        n = len(self._checked)
        label.update(f"{n} / {MAX_REVIEWERS} selected")
        label.set_class(n > MAX_REVIEWERS, "-over-limit")

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
