from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, RadioButton, RadioSet

from tissue.screens.base import TissueModal
from tissue.screens.project_home.rendering import _transition_label

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition


class TransitionPickerModal(TissueModal[int | None]):
    """Pick a workflow transition to perform on an issue.

    Each option reads `{name}: {current} → {target}` (via `_transition_label`);
    blocked transitions are shown disabled with their reason. Dismisses the chosen
    transition id, or `None` on cancel.
    """

    CSS_PATH = "transition_picker_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self,
        transitions: list[AvailableTransition],
        current_state_label: str,
        target_labels: dict[int, str],
    ) -> None:
        super().__init__()
        self._transitions = [t for t in transitions if t.transition_id is not None]
        self._current = current_state_label
        self._targets = target_labels

    def compose(self) -> ComposeResult:
        first_executable = next(
            (t.transition_id for t in self._transitions if t.can_execute), None
        )
        buttons = [
            RadioButton(
                _transition_label(t, self._current, self._targets),
                value=t.transition_id == first_executable,
                id=f"tr-{t.transition_id}",
                disabled=not t.can_execute,
            )
            for t in self._transitions
        ]
        with Container(id="tr-dialog", classes="dialog"):
            yield RadioSet(*buttons, id="tr-radio")
            with Horizontal(classes="tr-actions"):
                yield Button("Cancel", id="tr-cancel", classes="-btn-error")
                yield Button("Transition", id="tr-confirm", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#tr-dialog", Container)
        dialog.border_title = "Transition"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#tr-radio", RadioSet).focus()

    @on(Button.Pressed, "#tr-confirm")
    def _on_confirm(self) -> None:
        pressed = self.query_one("#tr-radio", RadioSet).pressed_button
        if pressed is None or pressed.id is None:
            return
        self.dismiss(int(pressed.id.removeprefix("tr-")))

    @on(Button.Pressed, "#tr-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)
