from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, VerticalScroll
from textual.widgets import Button

from tissue.screens.base import ScrollableModal
from tissue.screens.project_home.trash_render import trash_detail_widgets

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary


class TrashIssueModal(ScrollableModal["str | None"]):
    """Read a deleted issue. Read-only: the only action is Restore.

    Dismisses the issue key to restore, or None on close.
    """

    CSS_PATH = "trash_issue_modal.tcss"

    BINDINGS = [
        Binding("r", "restore", "restore"),
        Binding("escape", "close", "close"),
    ]

    def __init__(
        self,
        *,
        summary: IssueSummary,
        members: dict[int, str],
        state_colors: dict[int, str],
    ) -> None:
        super().__init__()
        self._summary = summary
        self._members = members
        self._state_colors = state_colors
        self._issue_key = summary.issue_key

    def compose(self) -> ComposeResult:
        with Container(id="tim-dialog", classes="dialog"):
            yield VerticalScroll(id="tim-scroll")
            with Horizontal(id="tim-actions"):
                yield Button("Restore", id="tim-restore", classes="trash-restore-btn")

    def on_mount(self) -> None:
        dialog = self.query_one("#tim-dialog", Container)
        dialog.border_title = self._issue_key or "Issue"
        dialog.border_subtitle = "r restore · Esc close"
        scroll = self.query_one("#tim-scroll", VerticalScroll)
        scroll.mount_all(
            trash_detail_widgets(
                self._summary,
                self._members,
                self._state_colors,
                self.app.theme_variables,
            )
        )

    def action_close(self) -> None:
        self.dismiss(None)

    def action_restore(self) -> None:
        self.dismiss(self._issue_key)

    @on(Button.Pressed, "#tim-restore")
    def _on_restore(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(self._issue_key)
