from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Label, SelectionList
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.screens.project_home.issue_filter import DEFAULT_ISSUE_FILTER, IssueFilter

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


class OpenIssueFilterModal(TissueModal["IssueFilter | None"]):
    """Filter the sprint detail's "Open issues" pool (state / priority / assignee).

    Closes with the chosen `IssueFilter`, or None if cancelled. Sprint/review/agent
    fields are left at their defaults since the pool has no use for them.
    """

    CSS_PATH = "open_issue_filter_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    _STATE_OPTIONS = (
        ("Initial", "INITIAL"),
        ("Active", "ACTIVE"),
        ("Completed", "COMPLETED"),
        ("Aborted", "ABORTED"),
    )
    _PRIORITY_OPTIONS = ("P0", "P1", "P2", "P3", "P4")

    def __init__(
        self, *, current: IssueFilter, members: list[ProjectMemberSummary]
    ) -> None:
        super().__init__()
        self._current = current
        self._assignee_all: list[tuple[str, str]] = [("Me", "me")]
        for member in members:
            if member.member_id is not None:
                self._assignee_all.append(
                    (
                        member.display_name or member.username or "-",
                        str(member.member_id),
                    )
                )

    def compose(self) -> ComposeResult:
        current = self._current
        with Container(id="ofm-dialog", classes="dialog"):
            with VerticalScroll(id="ofm-scroll"), Vertical(id="ofm-body"):
                yield Label("State", classes="ofm-label")
                yield SelectionList[str](
                    *(
                        Selection(label, value, value in current.state_categories)
                        for label, value in self._STATE_OPTIONS
                    ),
                    id="ofm-state",
                )
                yield Label("Priority", classes="ofm-label")
                yield SelectionList[str](
                    *(
                        Selection(p, p, p in current.priorities)
                        for p in self._PRIORITY_OPTIONS
                    ),
                    id="ofm-priority",
                )
                yield Label("Assignee", classes="ofm-label")
                yield SelectionList[str](
                    *(
                        Selection(
                            label,
                            value,
                            value in (current.assignee_member_ids or ()),
                        )
                        for label, value in self._assignee_all
                    ),
                    id="ofm-assignee",
                )
            with Horizontal(id="ofm-buttons"):
                yield Button("Reset", id="ofm-reset")
                yield Button("Cancel", id="ofm-cancel", classes="-btn-error")
                yield Button("Apply", id="ofm-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#ofm-dialog", Container)
        dialog.border_title = "Filter Open Issues"
        dialog.border_subtitle = "Esc to cancel"
        try:
            self.query_one("#ofm-state", SelectionList).focus()
        except NoMatches:
            pass

    @on(Button.Pressed, "#ofm-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        state = self.query_one("#ofm-state", SelectionList).selected
        priorities = self.query_one("#ofm-priority", SelectionList).selected
        assignees = self.query_one("#ofm-assignee", SelectionList).selected
        self.dismiss(
            IssueFilter(
                state_categories=tuple(state),
                priorities=tuple(priorities),
                assignee_member_ids=tuple(assignees) or None,
            )
        )

    @on(Button.Pressed, "#ofm-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#ofm-reset")
    def _on_reset(self, event: Button.Pressed) -> None:
        event.stop()
        state = self.query_one("#ofm-state", SelectionList)
        state.deselect_all()
        for value in DEFAULT_ISSUE_FILTER.state_categories:
            state.select(value)
        self.query_one("#ofm-priority", SelectionList).deselect_all()
        self.query_one("#ofm-assignee", SelectionList).deselect_all()

    def action_cancel(self) -> None:
        self.dismiss(None)
