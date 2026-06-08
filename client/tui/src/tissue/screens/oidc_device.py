import asyncio
import logging
import webbrowser

from textual import work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.i18n.manager import i18n
from tissue.models.auth import TokenPair
from tissue.screens.base import TissueModal
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)


class OidcDeviceModal(TissueModal[TokenPair | None]):
    """OIDC device-authorization login.

    Shows the user code + verification URL (and opens the browser), then polls
    the backend until the user authorizes at the IdP. Dismisses with a
    ``TokenPair`` on success, or ``None`` on cancel/failure.
    """

    CSS_PATH = "oidc_device.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    def __init__(self, idp: str) -> None:
        super().__init__()
        self.idp = idp
        self._spinner: Spinner | None = None
        self._cancelled = False

    def compose(self) -> ComposeResult:
        yield Container(
            Static(i18n.get("oidc_device_instruction"), id="oidc-instruction"),
            Static("", id="oidc-code"),
            Static("", id="oidc-url"),
            Static("", id="oidc-status"),
            Static(i18n.get("oidc_device_cancel_hint"), id="oidc-cancel-hint"),
            id="oidc-device-dialog",
            classes="dialog",
        )

    def on_mount(self) -> None:
        dialog = self.query_one("#oidc-device-dialog", Container)
        dialog.border_title = i18n.get("oidc_device_title", idp=self.idp)
        self._spinner = Spinner(self, self.query_one("#oidc-status", Static))
        self._run()

    def action_cancel(self) -> None:
        self._cancelled = True
        if self._spinner is not None:
            self._spinner.stop()
        self.dismiss(None)

    @work(exclusive=True)
    async def _run(self) -> None:
        client = self.app.client
        if client is None:
            self._fail("oidc_device_failed")
            return

        spinner = self._spinner
        assert spinner is not None
        spinner.start(i18n.get("oidc_device_starting"))

        try:
            start = await client.auth.oidc_device_start()
        except TissueApiError as e:
            log.warning("OIDC device start failed: %s", e)
            self._fail("oidc_device_failed")
            return
        if self._cancelled:
            return

        # Show the code + URL and try to open the browser (best effort).
        self.query_one("#oidc-code", Static).update(start.user_code)
        self.query_one("#oidc-url", Static).update(start.verification_uri)
        open_url = start.verification_uri_complete or start.verification_uri
        try:
            webbrowser.open(open_url)
        except Exception as e:
            log.debug("Could not open browser: %s", e)

        spinner.start(i18n.get("oidc_device_waiting"))

        interval = max(start.interval, 1)
        elapsed = 0
        while elapsed < start.expires_in:
            await asyncio.sleep(interval)
            elapsed += interval
            if self._cancelled:
                return
            try:
                poll = await client.auth.oidc_device_poll(start.device_code)
            except TissueApiError as e:
                log.debug("OIDC poll error (will retry): %s", e)
                continue

            status = (poll.status or "").upper()
            if status == "COMPLETE":
                if poll.access_token and poll.refresh_token:
                    spinner.stop()
                    self.dismiss(
                        TokenPair(
                            access_token=poll.access_token,
                            refresh_token=poll.refresh_token,
                        )
                    )
                    return
                self._fail("oidc_device_failed")
                return
            if status == "SLOW_DOWN":
                interval += 5
                continue
            if status == "PENDING":
                continue
            # DENIED / EXPIRED / ERROR
            self._fail(
                "oidc_device_denied" if status == "DENIED" else "oidc_device_failed"
            )
            return

        self._fail("oidc_device_expired")

    def _fail(self, message_key: str) -> None:
        if self._spinner is not None:
            self._spinner.stop()
        if self._cancelled:
            return
        self.app.notify(i18n.get(message_key), severity="error", timeout=5)
        self.dismiss(None)
