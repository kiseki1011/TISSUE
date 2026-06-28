from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Label, RadioButton, RadioSet, Static
from textual_timepiece.pickers import DateTimePicker

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.datetime_pickers import DueDateTimePicker as _DueDateTimePicker

_ACTION_LABELS = {
    "start": "Start  (Planning → Active)",
    "complete": "Complete  (Active → Completed)",
    "cancel": "Cancel sprint",
}


def available_sprint_actions(status: str) -> list[str]:
    status = (status or "").upper()
    if status == "PLANNING":
        return ["start", "cancel"]
    if status == "ACTIVE":
        return ["complete", "cancel"]
    return []


class SprintTransitionModal(TissueModal["bool | None"]):
    """Pick and run a sprint lifecycle action."""

    CSS_PATH = "sprint_transition_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(self, *, sprint_id: int, status: str) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._actions = available_sprint_actions(status)

    def compose(self) -> ComposeResult:
        with Container(id="str-dialog", classes="dialog"):
            yield RadioSet(
                *(
                    RadioButton(
                        _ACTION_LABELS[action], value=(index == 0), id=f"stra-{action}"
                    )
                    for index, action in enumerate(self._actions)
                ),
                id="str-radio",
            )
            with Horizontal(id="str-due-row"):
                yield Label("Due:", classes="str-due-label")
                yield _DueDateTimePicker(id="str-due")
            yield Static("", id="str-status", classes="status-msg")
            with Horizontal(classes="str-actions"):
                yield Button("Cancel", id="str-cancel", classes="-btn-error")
                yield Button("Confirm", id="str-confirm", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#str-dialog", Container)
        dialog.border_title = "Sprint action"
        dialog.border_subtitle = "Esc to cancel"
        picker = self.query_one("#str-due", DateTimePicker)
        self.watch(picker, "expanded", self._on_picker_expanded)
        self._sync_due()
        self.query_one("#str-radio", RadioSet).focus()

    def _selected(self) -> str | None:
        pressed = self.query_one("#str-radio", RadioSet).pressed_button
        if pressed is None or pressed.id is None:
            return None
        return pressed.id.removeprefix("stra-")

    def _sync_due(self) -> None:
        self.query_one("#str-due-row").display = self._selected() == "start"

    @on(RadioSet.Changed, "#str-radio")
    def _on_changed(self) -> None:
        self._sync_due()
        self._error("")

    def _on_picker_expanded(self, expanded: bool) -> None:
        self.query_one("#str-dialog", Container).set_class(bool(expanded), "-expanded")

    def _error(self, message: str) -> None:
        self.query_one("#str-status", Static).update(message)

    @on(Button.Pressed, "#str-confirm")
    def _on_confirm(self, event: Button.Pressed) -> None:
        event.stop()
        action = self._selected()
        if action is None:
            return
        due_at: str | None = None
        if action == "start":
            picked = self.query_one("#str-due", DateTimePicker).datetime
            if picked is None:
                self._error("Pick a due date to start.")
                return
            due_at = picked.assume_system_tz().to_instant().format_iso()
        self.run_worker(
            self._do_transition(action, due_at), exclusive=True, group="str"
        )

    async def _do_transition(self, action: str, due_at: str | None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if action == "start" and due_at is not None:
                await client.sprints.start_sprint(self._sprint_id, due_at=due_at)
            elif action == "complete":
                await client.sprints.complete_sprint(self._sprint_id)
            elif action == "cancel":
                await client.sprints.cancel_sprint(self._sprint_id)
        except TissueApiError as error:
            self._error(
                getattr(error, "detail", None) or str(error) or "Action failed."
            )
            return
        self.dismiss(True)

    @on(Button.Pressed, "#str-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)
