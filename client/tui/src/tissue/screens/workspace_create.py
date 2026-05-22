import logging

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult
from textual.widgets import Button, Input, Label, TextArea

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

# Backend revalidates with same regex rules
_WORKPSPACE_KEY_REGEX = "^[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$"

_AVAILABILITY_DEBOUNCE = 0.3
_DESCRIPTION_MAX_LENGTH = 255

_REQUIRED_FIELDS = ("ws_key", "ws_name")


class WorkspaceCreateModal(TissueModal[WorkspaceCreateResponse | None]):
    """Modal form for creating a new workspace."""

    CSS_PATH = "workspace_create.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self._key_check_timer: Timer | None = None
        self._key_available: bool | None = None

    def compose(self) -> ComposeResult:
        key_input = Input(
            placeholder=i18n.get("workspace_create_key_placeholder"),
            id="ws_key",
            classes="input-field",
            validators=[
                Length(
                    minimum=3,
                    maximum=22,
                    failure_description=i18n.get("workspace_key_length_violation"),
                ),
                Regex(
                    _WORKPSPACE_KEY_REGEX,
                    failure_description=i18n.get(
                        "workspace_create_key_regex_violation"
                    ),
                ),
            ],
            validate_on=["changed"],
        )
        key_input.border_title = i18n.get("workspace_create_key_label")

        name_input = Input(
            placeholder=i18n.get("workspace_create_name_placeholder"),
            id="ws_name",
            classes="input-field",
            validators=[
                Length(
                    minimum=2,
                    maximum=50,
                    failure_description=i18n.get("workspace_name_length_violation"),
                ),
            ],
            validate_on=["changed"],
        )
        name_input.border_title = i18n.get("workspace_create_name_label")

        description_input = TextArea(
            text="",
            placeholder=i18n.get("workspace_create_description_placeholder"),
            id="ws_description",
            classes="textarea-field",
            soft_wrap=True,
            show_line_numbers=False,
            compact=True,
        )
        description_input.border_title = i18n.get("workspace_create_description_label")

        form = Container(
            key_input,
            Label("", id="ws_key_status", classes="status-msg"),
            name_input,
            Label("", id="ws_name_status", classes="status-msg"),
            description_input,
            Label("", id="ws_description_status", classes="status-msg"),
            Button(
                i18n.get("workspace_create_submit_btn"),
                id="ws_create_btn",
                classes="-btn-success",
            ),
            id="ws-create-form",
        )
        dialog = Container(
            form,
            id="ws-create-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("workspace_create_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#ws_key", Input).focus()

    def action_close(self) -> None:
        self._stop_key_check_timer()
        self.dismiss(None)

    @on(Input.Changed)
    def on_input_changed(self, event: Input.Changed) -> None:
        """Update status label for the changed field."""
        input_id = event.input.id
        if input_id is None:
            return
        if input_id == "ws_key":
            self._on_key_changed(event)
        self._render_status(input_id, event.value, event.validation_result)

    def _on_key_changed(self, event: Input.Changed) -> None:
        """Reset availability state and reschedule the debounced API check.

        Only start check if the new value passes local validation.
        """
        self._key_available = None
        valid = self._format_valid(event)
        self._restart_key_check_timer(schedule=valid)

    @staticmethod
    def _format_valid(event: Input.Changed) -> bool:
        return bool(
            event.value.strip()
            and event.validation_result is not None
            and event.validation_result.is_valid
        )

    def _restart_key_check_timer(self, *, schedule: bool) -> None:
        """Cancel any pending key-check timer, optionally start a new one."""
        if self._key_check_timer is not None:
            self._key_check_timer.stop()
            self._key_check_timer = None
        if not schedule:
            return
        self._key_check_timer = self.set_timer(
            _AVAILABILITY_DEBOUNCE, self._do_check_key
        )

    def _stop_key_check_timer(self) -> None:
        if self._key_check_timer is not None:
            self._key_check_timer.stop()
            self._key_check_timer = None

    @work(exclusive=True, group="check_ws_key")
    async def _do_check_key(self) -> None:
        await self._check_key_availability()

    async def _check_key_availability(self) -> None:
        """Call the availability check API and update status label and state."""
        client = self.app.client
        if client is None:
            return

        inp = self.query_one("#ws_key", Input)
        value = inp.value.strip()
        if not value:
            return

        try:
            available = await client.workspaces.check_key_available(value)
        except TissueApiError as e:
            log.warning("Workspace key availability check failed: %s", e)
            self._set_status(
                "ws_key", i18n.get("workspace_create_key_check_failed"), "error"
            )
            return

        # Stale response guard
        # Skip if the input changed while awaiting
        if inp.value.strip() != value:
            return

        self._key_available = available
        if available:
            self._set_status(
                "ws_key", i18n.get("workspace_create_key_available"), "success"
            )
        else:
            self._set_status("ws_key", i18n.get("workspace_create_key_taken"), "error")

    def _render_status(
        self,
        input_id: str,
        value: str,
        result: ValidationResult | None,
    ) -> None:
        """Renders the validation result on the label."""
        if not value or result is None or result.is_valid:
            self._set_status(input_id)
            return
        msgs = result.failure_descriptions
        self._set_status(input_id, msgs[0] if msgs else "", "error")

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        """Replace a field's status label content and state class.

        `kind`:
            - "error" | "waiting" | "success" | None
            - `None` clears both the message and any state class.
        """
        label = self.query_one(f"#{input_id}_status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")

    @on(Button.Pressed, "#ws_create_btn")
    @on(Input.Submitted)
    def on_submit_pressed(self) -> None:
        if self._check_required_fields() is not None:
            return

        key = self.query_one("#ws_key", Input).value.strip()
        name = self.query_one("#ws_name", Input).value.strip()
        description = self.query_one("#ws_description", TextArea).text.strip()

        # TextArea has no built-in validators, so length is checked here
        if len(description) > _DESCRIPTION_MAX_LENGTH:
            self._set_status(
                "ws_description",
                i18n.get("workspace_description_length_violation"),
                "error",
            )
            self.query_one("#ws_description", TextArea).focus()
            return

        self._do_create(key, name, description or None)

    @on(TextArea.Changed, "#ws_description")
    def on_description_changed(self, event: TextArea.Changed) -> None:
        self._set_status("ws_description")

    def _check_required_fields(self) -> Input | None:
        """Find empty required fields, show error and focus first one."""
        first_empty: Input | None = None
        for fid in _REQUIRED_FIELDS:
            inp = self.query_one(f"#{fid}", Input)
            if not inp.value.strip():
                self._set_status(fid, i18n.get("login_validation_required"), "error")
                if first_empty is None:
                    first_empty = inp
        if first_empty is not None:
            first_empty.focus()
        return first_empty

    @work(exclusive=True, group="ws_create")
    async def _do_create(
        self, workspace_key: str, name: str, description: str | None
    ) -> None:
        """Call workspace create API.

        On failure notify (with reason).
        On success, notify and dismiss the modal with the created workspace response.
        """
        client = self.app.client
        if client is None:
            log.error("create workspace attempted without an authenticated client")
            return

        self.app.notify(i18n.get("workspace_create_submitting"), timeout=3)

        try:
            response = await client.workspaces.create(
                workspace_key=workspace_key, name=name, description=description
            )
        except TissueApiError as e:
            log.warning("Workspace creation failed: %s", e)
            self.app.notify(
                i18n.get(
                    "workspace_create_failed",
                    reason=self._workspace_create_failure_reason(e),
                ),
                severity="error",
                timeout=5,
            )
            return

        self.app.notify(i18n.get("workspace_create_success"), timeout=3)
        self._stop_key_check_timer()
        self.dismiss(response)

    @staticmethod
    def _workspace_create_failure_reason(exc: TissueApiError) -> str:
        """Map API error code to a human-friendly message.

        Falls back to the error's detail/title when no specific mapping exists.
        """
        if exc.title == "DUPLICATE_WORKSPACE_KEY":
            return i18n.get("workspace_create_error_duplicate_key")
        if exc.title == "INVALID_WORKSPACE_KEY_FORMAT":
            return i18n.get("workspace_create_error_invalid_key_format")
        if exc.title == "ACTIVE_MEMBER_NOT_FOUND":
            return i18n.get("workspace_create_error_active_member_not_found")
        if exc.title == "WORKSPACE_OWNAGE_LIMIT_EXCEEDED":
            return i18n.get("workspace_create_error_ownage_limit")
        if exc.title == "WORKSPACE_JOIN_LIMIT_EXCEEDED":
            return i18n.get("workspace_create_error_join_limit")
        if exc.title == "WORKSPACE_CREATE_ADMIN_ONLY":
            return i18n.get("workspace_create_error_forbidden")
        if exc.status == 403:
            return i18n.get("workspace_create_error_forbidden")
        return exc.detail or exc.title or str(exc)
