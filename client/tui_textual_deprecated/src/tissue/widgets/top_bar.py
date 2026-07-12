from typing import TYPE_CHECKING
from urllib.parse import urlparse

from rich.text import Text
from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.css.query import NoMatches
from textual.widgets import Label

from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TopBar(Horizontal):
    """Persistent top bar shown on every PostAuthScreen.

    Two regions: the server/user identity on the left, a screen-specific status
    on the right. Screens push the status via thㄷ screen's `set_top_bar_status()`.
    The cached value is read again on recompose.
    """

    DEFAULT_CSS = """
    TopBar {
        dock: top;
        width: 100%;
        height: 1;
        margin-top: 1;
        background: $background;
        padding: 0 2;
    }
    /* Identity left, status right; each takes half so a long status never pushes
       the identity off-screen. */
    TopBar .topbar-server {
        width: 1fr;
        padding: 0 1;
        color: $text-muted;
    }
    TopBar .topbar-status {
        width: 1fr;
        text-align: right;
        padding: 0 1;
        color: $text-muted;
    }
    """

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self, status: str = "") -> None:
        super().__init__()
        self._status = status

    def compose(self) -> ComposeResult:
        yield Label(self._server_label(), classes="topbar-server")
        yield Label(self._status, classes="topbar-status")

    def on_mount(self) -> None:
        # Re-render status dot whenever the realtime connection changes
        self.watch(self.app, "connection_state", self._on_connection_state, init=False)

    def _on_connection_state(self, _state: str) -> None:
        try:
            self.query_one(".topbar-server", Label).update(self._server_label())
        except NoMatches:
            pass

    def set_status(self, text: str) -> None:
        self.query_one(".topbar-status", Label).update(text)

    def _server_label(self) -> Text:
        """The server label on the header.

        - Server info
        - Status dot (color reflects the realtime connection)
        """
        info = self.app.system_info
        name = info.server_name if info is not None else None
        domain = self._server_domain()
        primary = color_hex(self.app.theme_variables.get("primary"))
        glyph, state_var = self._connection_indicator()
        dot = color_hex(self.app.theme_variables.get(state_var))
        label = Text()
        # Dot and server name share the connection color so the name reads as part
        # of the indicator (green connected, red disconnected, …).
        label.append(f"{glyph} ", style=dot or primary or "")
        label.append(name or domain or "tissue", style=dot or primary or "")

        if name and domain:
            label.append(f"  ·  {domain}", style="dim")
        username = self._username()
        if username:
            label.append(f"  ·  {username}", style="dim")
        return label

    def _connection_indicator(self) -> tuple[str, str]:
        """The status-dot glyph and its theme color variable for the connection."""
        state = getattr(self.app, "connection_state", "disconnected")
        if state == "connected":
            return "●", "success"
        if state == "connecting":
            return "◐", "warning"
        return "✕", "error"

    def _server_domain(self) -> str:
        client = self.app.client
        if client is None:
            return ""
        return urlparse(client.host).netloc or client.host

    def _username(self) -> str:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        if profile is None:
            return ""
        return profile.username or profile.email or ""
