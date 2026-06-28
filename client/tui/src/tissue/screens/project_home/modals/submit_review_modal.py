from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.screens.base import TissueModal


class SubmitReviewModal(TissueModal["bool | None"]):
    """Choose approve or request-changes for a review."""

    CSS_PATH = "submit_review_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(self, *, issue_key: str) -> None:
        super().__init__()
        self._issue_key = issue_key

    def compose(self) -> ComposeResult:
        with Container(id="srm-dialog", classes="dialog"):
            yield Static(
                f"Submit your review for {self._issue_key}.",
                classes="srm-prompt",
            )
            with Horizontal(id="srm-buttons"):
                yield Button("Cancel", id="srm-cancel")
                yield Button("Request changes", id="srm-reject", classes="-btn-error")
                yield Button("Approve", id="srm-approve", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#srm-dialog", Container)
        dialog.border_title = "Submit review"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#srm-approve", Button).focus()

    @on(Button.Pressed, "#srm-approve")
    def _on_approve(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(True)

    @on(Button.Pressed, "#srm-reject")
    def _on_reject(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(False)

    @on(Button.Pressed, "#srm-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)
