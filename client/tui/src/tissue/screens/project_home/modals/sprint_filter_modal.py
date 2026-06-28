from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Label
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.screens.project_home.sprint_filter import (
    DEFAULT_SPRINT_FILTER,
    SprintFilter,
)
from tissue.widgets.filter_selection_list import FilterSelectionList as SelectionList


class SprintFilterModal(TissueModal["SprintFilter | None"]):
    """Pick which sprint statuses the [1] Sprints list shows.

    Closes with the chosen `SprintFilter`, or None if cancelled.
    """

    CSS_PATH = "sprint_filter_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    _STATUS_OPTIONS = (
        ("Planning", "PLANNING"),
        ("Active", "ACTIVE"),
        ("Completed", "COMPLETED"),
        ("Cancelled", "CANCELLED"),
    )

    def __init__(self, *, current: SprintFilter) -> None:
        super().__init__()
        self._current = current

    def compose(self) -> ComposeResult:
        with Container(id="sfm-dialog", classes="dialog"):
            yield Label("Status", classes="sfm-label")
            yield SelectionList[str](
                *(
                    Selection(label, value, value in self._current.statuses)
                    for label, value in self._STATUS_OPTIONS
                ),
                id="sfm-status",
            )
            with Horizontal(id="sfm-buttons"):
                yield Button("Reset", id="sfm-reset")
                yield Button("Cancel", id="sfm-cancel", classes="-btn-error")
                yield Button("Apply", id="sfm-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#sfm-dialog", Container)
        dialog.border_title = "Filter Sprints"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#sfm-status", SelectionList).focus()

    @on(Button.Pressed, "#sfm-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        statuses = self.query_one("#sfm-status", SelectionList).selected
        self.dismiss(SprintFilter(statuses=tuple(statuses)))

    @on(Button.Pressed, "#sfm-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#sfm-reset")
    def _on_reset(self, event: Button.Pressed) -> None:
        event.stop()
        status = self.query_one("#sfm-status", SelectionList)
        status.deselect_all()
        for value in DEFAULT_SPRINT_FILTER.statuses:
            status.select(value)

    def action_cancel(self) -> None:
        self.dismiss(None)
