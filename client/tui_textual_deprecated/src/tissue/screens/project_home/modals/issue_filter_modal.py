from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Input, Label, Rule
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.screens.project_home.issue_filter import (
    DEFAULT_ISSUE_FILTER,
    IssueFilter,
)
from tissue.widgets.filter_checkbox import FilterCheckbox as Checkbox
from tissue.widgets.filter_selection_list import FilterSelectionList as SelectionList

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


class IssueFilterModal(TissueModal["IssueFilter | None"]):
    """Edit the [1] issue-list filter."""

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

    def compose(self) -> ComposeResult:
        current_filter = self._current
        with Container(id="filter-dialog", classes="dialog"):
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
                yield Checkbox(
                    "Current sprint only",
                    value=current_filter.current_sprint_only,
                    id="filter-current-sprint",
                )
                yield Rule()
                yield Label("Agent / Reviews filters", classes="filter-group")
                # When on, the State/Priority/Sprint above also limit the Agent
                # and Reviews lists.
                yield Checkbox(
                    "Apply the above filters to Agent / Reviews",
                    value=current_filter.apply_to_agent,
                    id="filter-agent",
                )
                # Only limits the Reviews view, no effect elsewhere.
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
        """Keep hidden checked assignees while the visible list is filtered."""
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
        self.query_one("#filter-current-sprint", Checkbox).value = False
        self.query_one("#filter-review-status", SelectionList).deselect_all()
        self.query_one("#filter-agent", Checkbox).value = False

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _collect(self) -> IssueFilter:
        self._sync_assignee_checked()  # catch a last click not handled yet
        state = self.query_one("#filter-state", SelectionList).selected
        priorities = self.query_one("#filter-priority", SelectionList).selected
        current_only = self.query_one("#filter-current-sprint", Checkbox).value
        review_statuses = self.query_one(
            "#filter-review-status", SelectionList
        ).selected
        agent = self.query_one("#filter-agent", Checkbox).value
        return IssueFilter(
            state_categories=tuple(state),
            priorities=tuple(priorities),
            assignee_member_ids=tuple(self._assignee_checked) or None,
            current_sprint_only=current_only,
            reviewer_statuses=tuple(review_statuses),
            apply_to_agent=bool(agent),
        )
