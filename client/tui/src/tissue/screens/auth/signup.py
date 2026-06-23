import logging

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Center, Container, Horizontal
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult, Validator
from textual.widgets import Button, Input, Label, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.screens.base import TissueScreen
from tissue.widgets.footer import TissueFooter
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)

_EMAIL_REGEX = r"[^@]+@[^@]+\.[^@]+"
_USERNAME_REGEX = r"^[a-z0-9]+$"
_PASSWORD_REGEX = r"^(?=.*[A-Za-z])(?=.*\d).+$"

_POLL_INTERVAL = 2.0
_AVAILABILITY_DEBOUNCE = 0.3

_UNIQUE_FIELDS = ("email", "username")
_REQUIRED_FIELDS = ("username", "name", "password")
_REQUIRED_FIELDS_EMAIL_REQUIRED = ("email", "username", "name", "password")


class _PasswordMatch(Validator):
    """Validator that checks if the confirm password equals the current password."""

    def __init__(self, screen: SignupScreen) -> None:
        super().__init__(failure_description="Passwords do not match")
        self._screen = screen

    def validate(self, value: str) -> ValidationResult:
        password = self._screen.query_one("#password", Input).value
        if value and value != password:
            return self.failure("Passwords do not match")
        return self.success()


class SignupScreen(TissueScreen):
    CSS_PATH = "signup.tcss"

    BINDINGS = [
        Binding("escape", "back", "back"),
    ]

    HORIZONTAL_BREAKPOINTS = [
        (0, "-h-narrow"),
        (78, "-h-medium"),
        (155, "-h-wide"),
    ]

    VERTICAL_BREAKPOINTS = [
        (0, "-v-short"),
        (42, "-v-tall"),
    ]

    def __init__(
        self, system_info: SystemInfoDetails, config_manager: ConfigManager
    ) -> None:
        super().__init__()

        self.system_info = system_info
        self.config_manager = config_manager
        self.email_required = self._email_required()

        # Email verification state
        self._verification_id: str | None = None
        self._verified_token: str | None = None
        self._poll_timer: Timer | None = None
        self._email_spinner: Spinner | None = None

        # Availability state by field id
        # None = not checked
        # True = available
        # False = already taken
        self._available: dict[str, bool | None] = dict.fromkeys(_UNIQUE_FIELDS)
        # Debounce timers by field id
        self._check_timers: dict[str, Timer | None] = dict.fromkeys(_UNIQUE_FIELDS)

    def compose(self) -> ComposeResult:
        server_url = self.config_manager.state.current_server_url or ""

        form_children = self._build_form_children()

        # Left pane: logo (centered) + server URL subtitle
        left_pane = Container(
            Center(Static(TISSUE_LOGO, classes="logo")),
            Label(f"Server: {server_url}", classes="dialog-subtitle"),
            id="left-pane",
        )

        # Right pane: signup form wrapped with container
        right_pane = Container(
            Container(*form_children, id="signup-form"),
            id="right-pane",
        )

        dialog = Container(
            left_pane,
            right_pane,
            id="dialog",
            classes="dialog",
        )
        dialog.border_title = "Sign up"

        yield dialog
        yield TissueFooter()

    def _build_form_children(self) -> list:
        """Build the ordered list of widgets that go inside signup form."""
        children: list = []

        if self.email_required:
            email_input = Input(
                placeholder="user@mycompany.com",
                id="email",
                classes="input-field",
                validators=[
                    Regex(
                        _EMAIL_REGEX,
                        failure_description="Invalid email format",
                    ),
                ],
                validate_on=["changed"],
            )
            email_input.border_title = "Email"

            # Disabled until availability check confirms the email is usable
            verify_btn = Button(
                "Verify",
                id="verify_btn",
                disabled=True,
            )

            children.extend(
                [
                    Horizontal(email_input, verify_btn, classes="email-row"),
                    Label("", id="email_status", classes="status-msg"),
                ]
            )

        username_input = Input(
            placeholder="yourusername",
            id="username",
            classes="input-field",
            validators=[
                Length(
                    minimum=3,
                    maximum=22,
                    failure_description="Must be 3-22 characters",
                ),
                Regex(
                    _USERNAME_REGEX,
                    failure_description="Only lowercase letters and digits",
                ),
            ],
            validate_on=["changed"],
        )
        username_input.border_title = "Username"

        name_input = Input(
            placeholder="Your name",
            id="name",
            classes="input-field",
            validators=[
                Length(
                    minimum=2,
                    maximum=35,
                    failure_description="Must be 2-35 characters",
                ),
            ],
            validate_on=["changed"],
        )
        name_input.border_title = "Name"

        password_input = Input(
            placeholder="********",
            password=True,
            id="password",
            classes="input-field",
            validators=[
                Length(
                    minimum=8,
                    maximum=30,
                    failure_description="Must be 8-30 characters",
                ),
                Regex(
                    _PASSWORD_REGEX,
                    failure_description="Must include letter and digit",
                ),
            ],
            validate_on=["changed"],
        )
        password_input.border_title = "Password"

        password_confirm_input = Input(
            placeholder="********",
            password=True,
            id="password_confirm",
            classes="input-field",
            validators=[_PasswordMatch(self)],
            validate_on=["changed"],
        )
        password_confirm_input.border_title = "Confirm password"

        children.extend(
            [
                username_input,
                Label("", id="username_status", classes="status-msg"),
                name_input,
                Label("", id="name_status", classes="status-msg"),
                password_input,
                Label("", id="password_status", classes="status-msg"),
                password_confirm_input,
                Label("", id="password_confirm_status", classes="status-msg"),
                Button(
                    "Create account",
                    id="signup_submit_btn",
                    # email-required=true: verification needed to activate submit button
                    disabled=self.email_required,
                    classes="-btn-success",
                ),
            ]
        )
        return children

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

        if self.email_required:
            self._email_spinner = Spinner(self, self.query_one("#email_status", Label))

        first_id = "#email" if self.email_required else "#username"
        self.query_one(first_id, Input).focus()

    def action_back(self) -> None:
        self._stop_poll()
        self._stop_check_timers()
        self.app.pop_screen()

    @on(Input.Changed)
    def on_input_changed(self, event: Input.Changed) -> None:
        """Handles all input change events. Uses the `event.input.id` to check which
        input was changed.

        Cases covered:
            - password changed
            - email changed
            - username changed
        """
        input_id = event.input.id
        if input_id is None:
            return
        if input_id == "password" and self.query_one("#password_confirm", Input).value:
            self._refresh_field_status("password_confirm")
        if input_id == "email":
            self._on_email_changed(event)
        elif input_id == "username":
            self._on_username_changed(event)

        self._render_status(input_id, event.value, event.validation_result)

    def _refresh_field_status(self, input_id: str) -> None:
        """Validate for a field and update its status label."""
        inp = self.query_one(f"#{input_id}", Input)
        self._render_status(input_id, inp.value, inp.validate(inp.value))

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

    def _on_email_changed(self, event: Input.Changed) -> None:
        """Reset verification state, restart availability check, lock submit."""
        self._stop_poll()
        self._verification_id = None
        self._verified_token = None
        self._available["email"] = None

        verify_btn = self.query_one("#verify_btn", Button)
        verify_btn.label = "Verify"
        verify_btn.disabled = True
        self.query_one("#signup_submit_btn", Button).disabled = True

        self._restart_check_timer("email", schedule=self._format_valid(event))

    def _on_username_changed(self, event: Input.Changed) -> None:
        self._available["username"] = None
        self._restart_check_timer("username", schedule=self._format_valid(event))

    def _format_valid(self, event: Input.Changed) -> bool:
        return bool(event.value) and (
            event.validation_result is None or event.validation_result.is_valid
        )

    def _restart_check_timer(self, field: str, *, schedule: bool) -> None:
        """Cancel any pending availability timer for the `field`, and optionally start
        a new one.

        Each keystroke calls this, so the timer keeps getting pushed forward until the
        user pauses for `_AVAILABILITY_DEBOUNCE` seconds. Then the actual API worker
        is called.
        """
        current = self._check_timers[field]
        if current is not None:
            current.stop()
            self._check_timers[field] = None
        if not schedule:
            return
        worker = self._do_check_email if field == "email" else self._do_check_username
        self._check_timers[field] = self.set_timer(_AVAILABILITY_DEBOUNCE, worker)

    @work(exclusive=True, group="check_email")
    async def _do_check_email(self) -> None:
        await self._check_availability("email")

    @work(exclusive=True, group="check_username")
    async def _do_check_username(self) -> None:
        await self._check_availability("username")

    async def _check_availability(self, field: str) -> None:
        """Call the availability check API and set the status."""
        client = self.app.client
        if client is None:
            return

        inp = self.query_one(f"#{field}", Input)
        value = inp.value.strip()
        if not value:
            return

        check_fn = (
            client.account.check_email_available
            if field == "email"
            else client.account.check_username_available
        )

        try:
            available = await check_fn(value)
        except TissueApiError as e:
            if inp.value.strip() != value:  # input changed while awaiting
                return
            log.warning("%s availability check failed: %s", field, e)
            check_failed = {
                "email": "Unable to check email availability",
                "username": "Unable to check username availability",
            }
            self._set_status(field, check_failed[field], "error")
            return

        # Skip if the input changed while awaiting
        if inp.value.strip() != value:
            return

        self._available[field] = available
        if available:
            self._set_status(field, "Available", "success")
            if field == "email":
                self.query_one("#verify_btn", Button).disabled = False
        else:
            taken = {
                "email": "Email already in use",
                "username": "Username already in use",
            }
            self._set_status(field, taken[field], "error")
            if field == "email":
                self.query_one("#verify_btn", Button).disabled = True

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        """Replace a field's status label content and state class.

        `kind`:
            - "error" | "waiting" | "success" | `None`
            - `None` clears both the message and any state class.
        """
        label = self._status_label(input_id)
        if label is None:
            return
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")

    def _status_label(self, input_id: str) -> Label | None:
        try:
            return self.query_one(f"#{input_id}_status", Label)
        except Exception:
            return None

    @on(Button.Pressed, "#verify_btn")
    def on_verify_pressed(self) -> None:
        email = self.query_one("#email", Input).value.strip()
        if not email:
            return
        self._do_request_verification(email)

    @work(exclusive=True, group="verify_request")
    async def _do_request_verification(self, email: str) -> None:
        """Request a verification email and start polling for verification status using
        verification id.
        """
        if self.app.client is None:
            return

        try:
            verification_id = await self.app.client.account.request_signup_verification(
                email
            )
        except TissueApiError as e:
            log.warning("Verification request failed: %s", e)
            self._set_status("email", "Failed to send verification email", "error")
            return

        self._verification_id = verification_id
        self.app.notify("Verification email sent. Check your inbox.", timeout=3)

        verify_btn = self.query_one("#verify_btn", Button)
        verify_btn.label = "Sent"
        verify_btn.disabled = True

        label = self._status_label("email")
        if label is not None:
            label.remove_class("-error", "-success")
            label.add_class("-waiting")
        if self._email_spinner is not None:
            self._email_spinner.start("Waiting for verification email")

        self._poll_timer = self.set_interval(_POLL_INTERVAL, self._poll_verification)

    @work(group="verify_poll")
    async def _poll_verification(self) -> None:
        """One polling tick. On `VERIFIED` status, stop polling and unlock submit."""
        verification_id = self._verification_id
        if verification_id is None or self.app.client is None:
            return

        try:
            status = await self.app.client.account.check_signup_verification(
                verification_id
            )
        except TissueApiError as e:
            log.warning("Verification status check failed: %s", e)
            return

        if status.status != "VERIFIED" or not status.verified_token:
            return

        self._verified_token = status.verified_token
        self._stop_poll()

        verify_btn = self.query_one("#verify_btn", Button)
        verify_btn.label = "✓"
        verify_btn.disabled = True

        self._set_status("email", "✓", "success")
        self.query_one("#signup_submit_btn", Button).disabled = False

    def _stop_poll(self) -> None:
        if self._poll_timer is not None:
            self._poll_timer.stop()
            self._poll_timer = None
        if self._email_spinner is not None:
            self._email_spinner.stop()

    def _stop_check_timers(self) -> None:
        for field, timer in self._check_timers.items():
            if timer is not None:
                timer.stop()
                self._check_timers[field] = None

    @on(Button.Pressed, "#signup_submit_btn")
    @on(Input.Submitted)
    def on_submit_pressed(self) -> None:
        if self._check_required_fields() is not None:
            return
        if self._available["username"] is False:
            self._set_status("username", "Username already in use", "error")
            self.query_one("#username", Input).focus()
            return
        if self.email_required and self._available["email"] is False:
            self._set_status("email", "Email already in use", "error")
            self.query_one("#email", Input).focus()
            return
        if self.email_required and not self._verified_token:
            self._set_status("email", "Email verification required", "error")
            return
        self._do_signup()

    def _check_required_fields(self) -> Input | None:
        """Find empty required fields, show error and focus first one."""
        ids = ["username", "name", "password", "password_confirm"]
        if self.email_required:
            ids.insert(0, "email")

        first_empty: Input | None = None
        for fid in ids:
            inp = self.query_one(f"#{fid}", Input)
            if not inp.value:
                self._set_status(fid, "Required field", "error")
                if first_empty is None:
                    first_empty = inp

        if first_empty is not None:
            first_empty.focus()
        return first_empty

    @work(exclusive=True, group="signup")
    async def _do_signup(self) -> None:
        """Call signup API.

        On success return to login, on failure notify the user.
        """
        if self.app.client is None:
            log.error("Signup attempted but TissueClient is not set")
            return

        email = (
            self.query_one("#email", Input).value.strip()
            if self.email_required
            else None
        )
        username = self.query_one("#username", Input).value.strip()
        name = self.query_one("#name", Input).value.strip()
        password = self.query_one("#password", Input).value

        self.app.notify("Creating account...", timeout=3)

        try:
            await self.app.client.account.signup(
                email=email,
                username=username,
                name=name,
                password=password,
                verified_token=self._verified_token,
            )
        except TissueApiError as e:
            log.warning("Signup failed: %s", e)
            self.app.notify(
                f"Sign up failed: {self._signup_failure_reason(e)}",
                severity="error",
                timeout=5,
            )
            return

        self.app.notify("Account created. Please log in.", timeout=3)
        self._stop_poll()
        self._stop_check_timers()
        self.app.pop_screen()

    @staticmethod
    def _signup_failure_reason(exc: TissueApiError) -> str:
        return exc.detail or exc.title or str(exc)

    def _email_required(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and setup.email_required)
