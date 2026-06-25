from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Checkbox, Input, Label, Rule, SelectionList
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.screens.project_home.issue_filter import (
    DEFAULT_ISSUE_FILTER,
    IssueFilter,
)

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )
    from tissue.api.generated.models.sprint_summary import SprintSummary

# Special value for "Current sprint", kept different from any real sprint id
# turned into text. Maps to the backend's current_sprint_only flag.
_CURRENT_SPRINT = "__current__"


class IssueFilterModal(TissueModal["IssueFilter | None"]):
    """Edit the filter for the issue list.

    Closes with the chosen `IssueFilter`, or None if cancelled.

    Searching assignees rebuilds the list, so checked picks are kept in
    `_assignee_checked` (which survives a search) instead of read from the live
    widget.
    """

    CSS_PATH = "issue_filter_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    _STATE_OPTIONS = (
        ("Initial", "INITIAL"),
        ("Active", "ACTIVE"),
        ("Completed", "COMPLETED"),
        ("Aborted", "ABORTED"),
    )
    _PRIORITY_OPTIONS = ("P0", "P1", "P2", "P3", "P4")
    _REVIEW_STATUS_OPTIONS = (
        ("Pending", "PENDING"),
        ("Changes requested", "CHANGES_REQUESTED"),
        ("Approved", "APPROVED"),
    )

    def __init__(
        self,
        *,
        current: IssueFilter,
        members: list[ProjectMemberSummary],
        sprints: list[SprintSummary],
    ) -> None:
        super().__init__()
        self._current = current
        self._sprints = sprints
        self._assignee_all: list[tuple[str, str]] = [("Me", "me")]
        for member in members:
            if member.member_id is not None:
                self._assignee_all.append(
                    (
                        member.display_name or member.username or "-",
                        str(member.member_id),
                    )
                )
        self._assignee_checked: set[str] = set(current.assignee_member_ids or ())
        self._assignee_keyword = ""
        # True while we rebuild the list ourselves. Clearing and re-adding items
        # fires change events that must not be treated as user clicks.
        self._rebuilding = False

    def _assignee_selections(self) -> list[Selection[str]]:
        keyword = self._assignee_keyword
        return [
            Selection(label, value, value in self._assignee_checked)
            for label, value in self._assignee_all
            if not keyword or keyword in label.casefold()
        ]

    def _sprint_selections(self) -> list[Selection[str]]:
        current_filter = self._current
        items: list[Selection[str]] = [
            Selection(
                "Current sprint", _CURRENT_SPRINT, current_filter.current_sprint_only
            )
        ]
        for sprint in self._sprints:
            if sprint.id is not None:
                items.append(
                    Selection(
                        sprint.title or sprint.sprint_key or "-",
                        str(sprint.id),
                        sprint.id in current_filter.sprint_ids,
                    )
                )
        return items

    def compose(self) -> ComposeResult:
        current_filter = self._current
        with Container(id="filter-dialog", classes="dialog"):
            # The scroll area is the dialog's full width so its scrollbar sits
            # right at the modal edge. The inner body holds the padding, matching
            # the buttons row.
            with VerticalScroll(id="filter-scroll"), Vertical(id="filter-body"):
                yield Label("[1] Issues filters", classes="filter-group")
                yield Label("State", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(
                            label, value, value in current_filter.state_categories
                        )
                        for label, value in self._STATE_OPTIONS
                    ),
                    id="filter-state",
                )
                yield Label("Priority", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(
                            priority, priority, priority in current_filter.priorities
                        )
                        for priority in self._PRIORITY_OPTIONS
                    ),
                    id="filter-priority",
                )
                yield Label("Assignee", classes="filter-label")
                yield Input(placeholder="Search members…", id="filter-assignee-search")
                yield SelectionList[str](
                    *self._assignee_selections(), id="filter-assignee"
                )
                yield Label("Sprint", classes="filter-label")
                yield SelectionList[str](*self._sprint_selections(), id="filter-sprint")
                yield Rule()
                yield Label("[3] filters", classes="filter-group")
                # When on, the [1] State/Priority/Sprint above also limit [3].
                yield Checkbox(
                    Text.assemble(
                        ("Apply ", ""),
                        ("[1]", "bold"),
                        (" filters to ", ""),
                        ("[3]", "bold"),
                    ),
                    value=current_filter.apply_to_agent,
                    id="filter-agent",
                )
                # Only limits the [3] Requested Reviews view, no effect elsewhere.
                yield Label("My Review Status", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(
                            label, value, value in current_filter.reviewer_statuses
                        )
                        for label, value in self._REVIEW_STATUS_OPTIONS
                    ),
                    id="filter-review-status",
                )
            with Horizontal(id="filter-buttons"):
                yield Button("Reset", id="filter-reset")
                yield Button("Cancel", id="filter-cancel", classes="-btn-error")
                yield Button("Apply", id="filter-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#filter-dialog", Container)
        dialog.border_title = "Filter Issues"
        dialog.border_subtitle = "Esc to cancel"
        try:
            self.query_one("#filter-state", SelectionList).focus()
        except NoMatches:
            pass

    @on(Input.Changed, "#filter-assignee-search")
    def _on_assignee_search(self, event: Input.Changed) -> None:
        # Save what is checked now, while the OLD search word still applies, so a
        # pick the user just clicked can't be lost by the rebuild regardless of
        # the order the click and search events arrive in.
        self._sync_assignee_checked()
        self._assignee_keyword = event.value.strip().casefold()
        self._rebuild_assignee()

    @on(SelectionList.SelectedChanged, "#filter-assignee")
    def _on_assignee_changed(self) -> None:
        if self._rebuilding:
            return
        self._sync_assignee_checked()

    def _sync_assignee_checked(self) -> None:
        """Save what is checked in the assignee list into our kept set.

        Removes the values shown right now, then adds back the checked ones, so
        picks hidden by the search aren't lost.
        """
        try:
            assignee_list = self.query_one("#filter-assignee", SelectionList)
        except NoMatches:
            return
        shown = {
            value
            for label, value in self._assignee_all
            if not self._assignee_keyword or self._assignee_keyword in label.casefold()
        }
        self._assignee_checked = (self._assignee_checked - shown) | set(
            assignee_list.selected
        )

    def _rebuild_assignee(self) -> None:
        try:
            assignee_list = self.query_one("#filter-assignee", SelectionList)
        except NoMatches:
            return
        self._rebuilding = True
        try:
            assignee_list.clear_options()
            assignee_list.add_options(self._assignee_selections())
        finally:
            self._rebuilding = False

    @on(Button.Pressed, "#filter-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(self._collect())

    @on(Button.Pressed, "#filter-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#filter-reset")
    def _on_reset(self, event: Button.Pressed) -> None:
        event.stop()
        state = self.query_one("#filter-state", SelectionList)
        state.deselect_all()
        for value in DEFAULT_ISSUE_FILTER.state_categories:
            state.select(value)
        self.query_one("#filter-priority", SelectionList).deselect_all()
        self._assignee_checked = set()
        self._assignee_keyword = ""
        self.query_one("#filter-assignee-search", Input).value = ""
        self._rebuild_assignee()
        self.query_one("#filter-sprint", SelectionList).deselect_all()
        self.query_one("#filter-review-status", SelectionList).deselect_all()
        self.query_one("#filter-agent", Checkbox).value = False

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _collect(self) -> IssueFilter:
        self._sync_assignee_checked()  # catch a last click not handled yet
        state = self.query_one("#filter-state", SelectionList).selected
        priorities = self.query_one("#filter-priority", SelectionList).selected
        sprint_sel = set(self.query_one("#filter-sprint", SelectionList).selected)
        current_only = _CURRENT_SPRINT in sprint_sel
        sprint_ids = tuple(
            int(value) for value in sprint_sel if value != _CURRENT_SPRINT
        )
        review_statuses = self.query_one(
            "#filter-review-status", SelectionList
        ).selected
        agent = self.query_one("#filter-agent", Checkbox).value
        return IssueFilter(
            state_categories=tuple(state),
            priorities=tuple(priorities),
            assignee_member_ids=tuple(self._assignee_checked) or None,
            sprint_ids=sprint_ids,
            current_sprint_only=current_only,
            reviewer_statuses=tuple(review_statuses),
            apply_to_agent=bool(agent),
        )
