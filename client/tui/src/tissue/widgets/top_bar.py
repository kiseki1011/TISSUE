from typing import TYPE_CHECKING
from urllib.parse import urlparse

from rich.text import Text
from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.widgets import Label

from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TopBar(Horizontal):
    """Persistent top bar shown on every PostAuthScreen.

    Two regions: the server/user identity on the left, a screen-specific status
    (e.g. assigned-issue counts) on the right. Screens push the status via the
    screen's set_top_bar_status(); the cached value is re-read on recompose.
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

    def set_status(self, text: str) -> None:
        self.query_one(".topbar-status", Label).update(text)

    def _server_label(self) -> Text:
        info = self.app.system_info
        name = info.server_name if info is not None else None
        domain = self._server_domain()
        # A filled dot + the server identity, in the primary color. color_hex
        # falls back to "" on ANSI themes (Rich rejects ansi_* names), leaving
        # the identity at the muted base color rather than crashing.
        primary = color_hex(self.app.theme_variables.get("primary"))
        label = Text()
        label.append(f"● {name or domain or 'tissue'}", style=primary or "")
        # Domain then username trail the identity in one left-aligned run, same
        # dim ·-separated format, instead of pinning the user to the far edge.
        if name and domain:
            label.append(f"  ·  {domain}", style="dim")
        username = self._username()
        if username:
            label.append(f"  ·  {username}", style="dim")
        return label

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
