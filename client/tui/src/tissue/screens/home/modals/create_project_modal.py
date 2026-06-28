import logging
import re
from dataclasses import dataclass

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult
from textual.widgets import Button, Input, Label, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.form_helpers import render_validation_status, set_field_status

log = logging.getLogger(__name__)

_KEY_REGEX = r"^[A-Z]+[0-9]*$"
_KEY_MIN, _KEY_MAX = 2, 10
_TITLE_MIN, _TITLE_MAX = 2, 60
_DESC_MAX = 255
_AVAILABILITY_DEBOUNCE = 0.3


@dataclass(frozen=True)
class _ProjectFormValues:
    key: str
    title: str
    description: str


class CreateProjectModal(TissueModal[str | None]):
    """Create a project, dismissing with the new project key on success.

    The key field debounces a uniqueness and reserved check against the server,
    like signup's username check. The actual create still validates server-side.
    """

    CSS_PATH = "create_project_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self._submitting = False
        self._key_check_timer: Timer | None = None
        # Availability of the entered key.
        #   - None -> not yet checked
        #   - True -> available
        #   - False -> taken or reserved
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
                classes="-btn-error",
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

    def on_unmount(self) -> None:
        self._stop_key_check_timer()

    def action_close(self) -> None:
        self._stop_key_check_timer()
        self.dismiss(None)

    @on(Input.Changed)
    def _on_input_changed(self, event: Input.Changed) -> None:
        input_id = event.input.id
        if input_id is None:
            return
        # Project keys are uppercase by policy, so fold input as the user types.
        if input_id == "project_create_key":
            upper = event.value.upper()
            if upper != event.value:
                cursor_position = event.input.cursor_position
                event.input.value = upper  # re-fires Changed with the folded value
                event.input.cursor_position = cursor_position
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
            self._set_status("project_create_key")  # availability shown after debounce
        else:
            self._render_status(
                "project_create_key", event.value, event.validation_result
            )
        self._restart_key_check(schedule=format_ok)

    def _restart_key_check(self, *, schedule: bool) -> None:
        self._stop_key_check_timer()
        if schedule:
            self._key_check_timer = self.set_timer(
                _AVAILABILITY_DEBOUNCE, self._do_check_key
            )

    def _stop_key_check_timer(self) -> None:
        if self._key_check_timer is not None:
            self._key_check_timer.stop()
            self._key_check_timer = None

    @work(exclusive=True, group="check_project_key")
    async def _do_check_key(self) -> None:
        client = self.app.client
        if client is None:
            return
        key_input = self.query_one("#project_create_key", Input)
        value = key_input.value.strip()
        if not value:
            return
        # No "checking..." status. The check is usually fast, so showing it would
        # just flicker before the result. Leave the field blank until the result.
        try:
            result = await client.projects.check_project_key(value)
        except TissueApiError as error:
            if key_input.value.strip() != value:  # input changed while awaiting
                return
            log.warning("Project key check failed: %s", error)
            self._key_available = None
            self._set_status(
                "project_create_key",
                "Couldn't check key availability.",
                "error",
            )
            return

        if key_input.value.strip() != value:  # input changed while awaiting
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
        else:
            self._key_available = False
            self._set_status(
                "project_create_key", "That key is already taken.", "error"
            )

    @on(Button.Pressed, "#project_create_cancel_btn")
    def _on_cancel_pressed(self) -> None:
        self.action_close()

    @on(Button.Pressed, "#project_create_submit_btn")
    @on(Input.Submitted)
    def _on_submit(self) -> None:
        if self._submitting:
            return
        values = self._read_form_values()
        if not self._validate_form(values):
            return

        # The debounce already confirmed this key is unusable.
        if self._key_available is False:
            self.query_one("#project_create_key", Input).focus()
            return

        self._submitting = True
        self.query_one("#project_create_submit_btn", Button).disabled = True
        self._do_create(
            key=values.key,
            title=values.title,
            description=values.description or None,
        )

    def _read_form_values(self) -> _ProjectFormValues:
        return _ProjectFormValues(
            key=self.query_one("#project_create_key", Input).value.strip(),
            title=self.query_one("#project_create_title", Input).value.strip(),
            description=self.query_one("#project_create_desc", TextArea).text.strip(),
        )

    def _validate_form(self, values: _ProjectFormValues) -> bool:
        valid = True
        if not values.key:
            self._set_status("project_create_key", "Required field", "error")
            valid = False
        elif not (
            _KEY_MIN <= len(values.key) <= _KEY_MAX
            and re.fullmatch(_KEY_REGEX, values.key)
        ):
            self._set_status(
                "project_create_key",
                "2-10 uppercase letters, optional digits (e.g. DEMO)",
                "error",
            )
            valid = False

        if not values.title:
            self._set_status("project_create_title", "Required field", "error")
            valid = False
        elif not (_TITLE_MIN <= len(values.title) <= _TITLE_MAX):
            self._set_status(
                "project_create_title",
                "2-60 characters",
                "error",
            )
            valid = False

        if len(values.description) > _DESC_MAX:
            self._set_status("project_create_desc", "Up to 255 characters", "error")
            valid = False
        return valid

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
        except TissueApiError as error:
            if error.status == 409:
                self._fail_on_key("project_create_key_taken")
                return
            if error.title == "RESERVED_PROJECT_KEY":
                self._fail_on_key("project_create_key_reserved")
                return
            log.warning("Project create failed: %s", error)
            self._reset_submitting()
            self.app.notify(
                f"Failed to create project: {self._failure_reason(error)}",
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
        """Show a key-specific error inline and re-enable submission."""
        self._key_available = False
        self._reset_submitting()
        message = self._KEY_FAILURE_MESSAGES[message_key]
        self._set_status("project_create_key", message, "error")
        self.query_one("#project_create_key", Input).focus()

    def _reset_submitting(self) -> None:
        self._submitting = False
        self.query_one("#project_create_submit_btn", Button).disabled = False

    @staticmethod
    def _failure_reason(error: TissueApiError) -> str:
        return error.detail or error.title or str(error)

    def _render_status(
        self, input_id: str, value: str, result: ValidationResult | None
    ) -> None:
        render_validation_status(self, input_id, value, result)

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        set_field_status(self, input_id, message, kind)
