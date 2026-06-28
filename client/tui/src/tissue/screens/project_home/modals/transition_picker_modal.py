from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.widgets import Button, RadioButton, RadioSet, Static

from tissue.screens.base import TissueModal
from tissue.screens.project_home.rendering import _transition_label

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition


class TransitionPickerModal(TissueModal[int | None]):
    """Pick a workflow transition.

    Dismisses the chosen transition id, or `None` on cancel.
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
        self._transitions = [
            transition
            for transition in transitions
            if transition.transition_id is not None
        ]
        self._current_state_label = current_state_label
        self._target_labels = target_labels

    def compose(self) -> ComposeResult:
        first_executable = next(
            (
                transition.transition_id
                for transition in self._transitions
                if transition.can_execute
            ),
            None,
        )
        buttons = [
            RadioButton(
                _transition_label(
                    transition, self._current_state_label, self._target_labels
                ),
                value=transition.transition_id == first_executable,
                id=f"tr-{transition.transition_id}",
                disabled=not transition.can_execute,
            )
            for transition in self._transitions
        ]
        with Container(id="tr-dialog", classes="dialog"):
            yield RadioSet(*buttons, id="tr-radio")
            yield from self._blocked_widgets()
            with Horizontal(classes="tr-actions"):
                yield Button("Cancel", id="tr-cancel", classes="-btn-error")
                yield Button("Transition", id="tr-confirm", classes="-btn-success")

    def _blocked_widgets(self) -> ComposeResult:
        """One warning block per blocked transition: its name then a line per reason.

        Kept out of the RadioSet (whose buttons are a fixed one line tall and whose
        arrow-key navigation would trip over non-button children).
        """
        for transition in self._transitions:
            if transition.can_execute or not transition.blocked_reasons:
                continue
            messages = [
                reason.message
                for reason in transition.blocked_reasons
                if reason.message
            ]
            if not messages:
                continue
            rows: list[Static] = [
                Static(
                    f"⚠ {transition.display_label or '?'}",
                    markup=False,
                    classes="tr-blocked-title",
                )
            ]
            rows += [
                Static(f"• {message}", markup=False, classes="tr-blocked-reason")
                for message in messages
            ]
            yield Vertical(*rows, classes="tr-blocked")

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
