"""Modal for creating a new project."""

import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult
from textual.widgets import Button, Input, Label, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

_KEY_REGEX = r"^[A-Z]+[0-9]*$"
_KEY_MIN, _KEY_MAX = 2, 10
_TITLE_MIN, _TITLE_MAX = 2, 60
_DESC_MAX = 255
_AVAILABILITY_DEBOUNCE = 0.3


class CreateProjectModal(TissueModal[str | None]):
    """Create a project. Dismisses with the new project key on success.

    The key field debounces a uniqueness/reserved check against the server
    (like signup's username check); the actual create still validates server-side.
    """

    CSS_PATH = "create_project_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self._submitting = False
        self._key_check_timer: Timer | None = None
        # None = unknown/unchecked, True = available, False = taken/reserved
        self._key_available: bool | None = None

    def compose(self) -> ComposeResult:
        key = Input(
            placeholder="DEMO",
            id="project_create_key",
            classes="input-field",
            validators=[
                Length(
                    minimum=_KEY_MIN,
                    maximum=_KEY_MAX,
                    failure_description=(
                        "2-10 uppercase letters, optional digits (e.g. DEMO)"
                    ),
                ),
                Regex(
                    _KEY_REGEX,
                    failure_description=(
                        "2-10 uppercase letters, optional digits (e.g. DEMO)"
                    ),
                ),
            ],
            validate_on=["changed"],
        )
        key.border_title = "Project key"

        title = Input(
            placeholder="Demo Project",
            id="project_create_title",
            classes="input-field",
            validators=[
                Length(
                    minimum=_TITLE_MIN,
                    maximum=_TITLE_MAX,
                    failure_description="2-60 characters",
                ),
            ],
            validate_on=["changed"],
        )
        title.border_title = "Title"

        description = TextArea(
            id="project_create_desc",
            classes="desc-area",
            soft_wrap=True,
            tab_behavior="focus",
            show_line_numbers=False,
            placeholder="What is this project about?",
        )
        description.border_title = "Description (optional)"

        buttons = Horizontal(
            Button(
                "Cancel",
                id="project_create_cancel_btn",
            ),
            Button(
                "Create",
                id="project_create_submit_btn",
                classes="-btn-success",
            ),
            id="create-project-buttons",
        )
        form = Container(
            key,
            Label("", id="project_create_key_status", classes="status-msg"),
            title,
            Label("", id="project_create_title_status", classes="status-msg"),
            description,
            Label("", id="project_create_desc_status", classes="status-msg"),
            buttons,
            id="create-project-form",
        )
        dialog = Container(form, id="create-project-dialog", classes="dialog")
        dialog.border_title = "New project"
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#project_create_key", Input).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    # ---- input handling -------------------------------------------------

    @on(Input.Changed)
    def _on_input_changed(self, event: Input.Changed) -> None:
        input_id = event.input.id
        if input_id is None:
            return
        # Project keys are uppercase by policy — fold input as the user types.
        if input_id == "project_create_key":
            upper = event.value.upper()
            if upper != event.value:
                pos = event.input.cursor_position
                event.input.value = upper  # re-fires Changed with the folded value
                event.input.cursor_position = pos
                return
            self._on_key_changed(event)
            return
        self._render_status(input_id, event.value, event.validation_result)

    def _on_key_changed(self, event: Input.Changed) -> None:
        value = event.value.strip()
        format_ok = (
            bool(value)
            and _KEY_MIN <= len(value) <= _KEY_MAX
            and re.fullmatch(_KEY_REGEX, value) is not None
        )
        self._key_available = None
        if format_ok:
            self._set_status("project_create_key")  # clear; availability after debounce
        else:
            self._render_status(
                "project_create_key", event.value, event.validation_result
            )
        self._restart_key_check(schedule=format_ok)

    def _restart_key_check(self, *, schedule: bool) -> None:
        if self._key_check_timer is not None:
            self._key_check_timer.stop()
            self._key_check_timer = None
        if schedule:
            self._key_check_timer = self.set_timer(
                _AVAILABILITY_DEBOUNCE, self._do_check_key
            )

    @work(exclusive=True, group="check_project_key")
    async def _do_check_key(self) -> None:
        client = self.app.client
        if client is None:
            return
        inp = self.query_one("#project_create_key", Input)
        value = inp.value.strip()
        if not value:
            return
        # No "checking..." status: the check is usually fast, so showing it would
        # just flicker before the result. Leave the field blank until the result.
        try:
            result = await client.projects.check_project_key(value)
        except TissueApiError as e:
            if inp.value.strip() != value:  # input changed while awaiting
                return
            log.warning("Project key check failed: %s", e)
            self._key_available = None
            self._set_status(
                "project_create_key",
                "Couldn't check key availability.",
                "error",
            )
            return

        if inp.value.strip() != value:  # input changed while awaiting
            return

        if result == "available":
            self._key_available = True
            self._set_status(
                "project_create_key",
                "Available",
                "success",
            )
        elif result == "reserved":
            self._key_available = False
            self._set_status(
                "project_create_key", "That key is reserved by the system.", "error"
            )
        else:  # "taken"
            self._key_available = False
            self._set_status(
                "project_create_key", "That key is already taken.", "error"
            )

    # ---- submit ---------------------------------------------------------

    @on(Button.Pressed, "#project_create_cancel_btn")
    def _on_cancel_pressed(self) -> None:
        self.action_close()

    @on(Button.Pressed, "#project_create_submit_btn")
    @on(Input.Submitted)
    def _on_submit(self) -> None:
        if self._submitting:
            return
        key = self.query_one("#project_create_key", Input).value.strip()
        title = self.query_one("#project_create_title", Input).value.strip()
        description = self.query_one("#project_create_desc", TextArea).text.strip()

        ok = True
        if not key:
            self._set_status("project_create_key", "Required field", "error")
            ok = False
        elif not (_KEY_MIN <= len(key) <= _KEY_MAX and re.fullmatch(_KEY_REGEX, key)):
            self._set_status(
                "project_create_key",
                "2-10 uppercase letters, optional digits (e.g. DEMO)",
                "error",
            )
            ok = False

        if not title:
            self._set_status("project_create_title", "Required field", "error")
            ok = False
        elif not (_TITLE_MIN <= len(title) <= _TITLE_MAX):
            self._set_status(
                "project_create_title",
                "2-60 characters",
                "error",
            )
            ok = False

        if len(description) > _DESC_MAX:
            self._set_status("project_create_desc", "Up to 255 characters", "error")
            ok = False

        if not ok:
            return

        # The debounce already confirmed this key is unusable.
        if self._key_available is False:
            self.query_one("#project_create_key", Input).focus()
            return

        self._submitting = True
        self.query_one("#project_create_submit_btn", Button).disabled = True
        self._do_create(key=key, title=title, description=description or None)

    @work(exclusive=True, group="create_project")
    async def _do_create(
        self, *, key: str, title: str, description: str | None
    ) -> None:
        client = self.app.client
        if client is None:
            log.error("Project create attempted but TissueClient is not set")
            self._reset_submitting()
            return
        try:
            response = await client.projects.create_project(
                project_key=key, title=title, description=description
            )
        except TissueApiError as e:
            if e.status == 409:
                self._fail_on_key("project_create_key_taken")
                return
            if e.title == "RESERVED_PROJECT_KEY":
                self._fail_on_key("project_create_key_reserved")
                return
            log.warning("Project create failed: %s", e)
            self._reset_submitting()
            self.app.notify(
                f"Failed to create project: {self._failure_reason(e)}",
                severity="error",
            )
            return

        created = response.project_key or key
        self.app.notify(f"Project {created} created.")
        self.dismiss(created)

    _KEY_FAILURE_MESSAGES = {
        "project_create_key_taken": "That key is already taken.",
        "project_create_key_reserved": "That key is reserved by the system.",
    }

    def _fail_on_key(self, message_key: str) -> None:
        """Surface a key-specific error inline and re-enable submission."""
        self._key_available = False
        self._reset_submitting()
        message = self._KEY_FAILURE_MESSAGES[message_key]
        self._set_status("project_create_key", message, "error")
        self.query_one("#project_create_key", Input).focus()

    def _reset_submitting(self) -> None:
        self._submitting = False
        self.query_one("#project_create_submit_btn", Button).disabled = False

    @staticmethod
    def _failure_reason(exc: TissueApiError) -> str:
        return exc.detail or exc.title or str(exc)

    def _render_status(
        self, input_id: str, value: str, result: ValidationResult | None
    ) -> None:
        if not value or result is None or result.is_valid:
            self._set_status(input_id)
            return
        msgs = result.failure_descriptions
        self._set_status(input_id, msgs[0] if msgs else "", "error")

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        label = self.query_one(f"#{input_id}_status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")
