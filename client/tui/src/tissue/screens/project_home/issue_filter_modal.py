"""The [1] Issues list's filter modal (opened by the ⚙ button beside the search
bar): pick state categories, priorities, assignees and sprints (all OR-matched),
optionally the current sprint, and whether the same narrowing applies to the [3]
Agent Work box. Dismisses with the chosen `IssueFilter`, or None if cancelled."""

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

# Sentinel value for the "Current sprint" entry in the sprint list — distinct from
# any real (stringified) sprint id, mapped to the backend's current_sprint_only flag.
_CURRENT_SPRINT = "__current__"


class IssueFilterModal(TissueModal["IssueFilter | None"]):
    """Filter editor for the issue list. Pre-fills from the current filter.

    Assignees and sprints are multi-select (OR). The assignee list has a client-side
    search box; because searching rebuilds the list (dropping filtered-out rows), the
    checked set is tracked in `_assignee_checked` (the source of truth that survives
    filtering) rather than read from the live widget until Apply.
    """

    CSS_PATH = "issue_filter_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    # (display label, backend StateCategory value).
    _STATE_OPTIONS = (
        ("Initial", "INITIAL"),
        ("Active", "ACTIVE"),
        ("Completed", "COMPLETED"),
        ("Aborted", "ABORTED"),
    )
    _PRIORITY_OPTIONS = ("P0", "P1", "P2", "P3", "P4")
    # (display label, backend ReviewStatus value).
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
        # Assignee picks: "Me" plus each member by name. The search box filters this
        # list client-side; `_assignee_checked` holds the selection across rebuilds.
        self._assignee_all: list[tuple[str, str]] = [("Me", "me")]
        for m in members:
            if m.member_id is not None:
                self._assignee_all.append(
                    (m.display_name or m.username or "-", str(m.member_id))
                )
        self._assignee_checked: set[str] = set(current.assignee_member_ids or ())
        self._assignee_kw = ""
        # Guards the SelectedChanged handler while we programmatically rebuild the
        # assignee list (clear/add fires events we must not treat as user toggles).
        self._rebuilding = False

    def _assignee_selections(self) -> list[Selection[str]]:
        kw = self._assignee_kw
        return [
            Selection(label, value, value in self._assignee_checked)
            for label, value in self._assignee_all
            if not kw or kw in label.casefold()
        ]

    def _sprint_selections(self) -> list[Selection[str]]:
        cur = self._current
        items: list[Selection[str]] = [
            Selection("Current sprint", _CURRENT_SPRINT, cur.current_sprint_only)
        ]
        for s in self._sprints:
            if s.id is not None:
                items.append(
                    Selection(
                        s.title or s.sprint_key or "-",
                        str(s.id),
                        s.id in cur.sprint_ids,
                    )
                )
        return items

    def compose(self) -> ComposeResult:
        cur = self._current
        with Container(id="filter-dialog", classes="dialog"):
            # The scroll spans the dialog's full width (no horizontal padding on the
            # dialog) so its scrollbar sits flush at the modal edge; the inner body
            # carries the left/right padding, kept equal to the buttons row's.
            with VerticalScroll(id="filter-scroll"), Vertical(id="filter-body"):
                # Group 1 — filters that narrow the [1] Issues list.
                yield Label("[1] Issues filters", classes="filter-group")
                yield Label("State", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(label, value, value in cur.state_categories)
                        for label, value in self._STATE_OPTIONS
                    ),
                    id="filter-state",
                )
                yield Label("Priority", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(p, p, p in cur.priorities)
                        for p in self._PRIORITY_OPTIONS
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
                # Group 2 — filters scoped to the [3] Agent / Requested Reviews box.
                yield Rule()
                yield Label("[3] filters", classes="filter-group")
                # When on, the [1] State/Priority/Sprint above also narrow [3].
                yield Checkbox(
                    Text.assemble(
                        ("Apply ", ""),
                        ("[1]", "bold"),
                        (" filters to ", ""),
                        ("[3]", "bold"),
                    ),
                    value=cur.apply_to_agent,
                    id="filter-agent",
                )
                # Only narrows the [3] Requested Reviews view (no effect elsewhere).
                yield Label("My Review Status", classes="filter-label")
                yield SelectionList[str](
                    *(
                        Selection(label, value, value in cur.reviewer_statuses)
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
        # Fold the live selection in under the OLD keyword before the filter changes,
        # so a just-toggled pick can't be dropped by the rebuild regardless of how the
        # toggle/search events interleave.
        self._sync_assignee_checked()
        self._assignee_kw = event.value.strip().casefold()
        self._rebuild_assignee()

    @on(SelectionList.SelectedChanged, "#filter-assignee")
    def _on_assignee_changed(self) -> None:
        if self._rebuilding:
            return
        self._sync_assignee_checked()

    def _sync_assignee_checked(self) -> None:
        """Fold the live assignee selection into the tracked set. Merge: drop the
        currently-shown values, then re-add whatever is checked among them — so
        filtered-out (hidden) picks survive. Called on every toggle and again at
        collect time, so the result never depends on event-queue timing."""
        try:
            select = self.query_one("#filter-assignee", SelectionList)
        except NoMatches:
            return
        shown = {
            value
            for label, value in self._assignee_all
            if not self._assignee_kw or self._assignee_kw in label.casefold()
        }
        self._assignee_checked = (self._assignee_checked - shown) | set(select.selected)

    def _rebuild_assignee(self) -> None:
        try:
            select = self.query_one("#filter-assignee", SelectionList)
        except NoMatches:
            return
        self._rebuilding = True
        try:
            select.clear_options()
            select.add_options(self._assignee_selections())
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
        self._assignee_kw = ""
        self.query_one("#filter-assignee-search", Input).value = ""
        self._rebuild_assignee()
        self.query_one("#filter-sprint", SelectionList).deselect_all()
        self.query_one("#filter-review-status", SelectionList).deselect_all()
        self.query_one("#filter-agent", Checkbox).value = False

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _collect(self) -> IssueFilter:
        self._sync_assignee_checked()  # reflect any not-yet-processed final toggle
        state = self.query_one("#filter-state", SelectionList).selected
        priorities = self.query_one("#filter-priority", SelectionList).selected
        sprint_sel = set(self.query_one("#filter-sprint", SelectionList).selected)
        current_only = _CURRENT_SPRINT in sprint_sel
        sprint_ids = tuple(int(v) for v in sprint_sel if v != _CURRENT_SPRINT)
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
