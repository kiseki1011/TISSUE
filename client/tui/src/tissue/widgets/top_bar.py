from typing import TYPE_CHECKING
from urllib.parse import urlparse

from rich.text import Text
from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.widgets import Label

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TopBar(Horizontal):
    """Persistent top bar shown on every PostAuthScreen.

    Three regions:
        - server, left
        - breadcrumb, center
        - user, right

    The breadcrumb tells the user where they are and the username tells them
    who they are.
    """

    DEFAULT_CSS = """
    TopBar {
        dock: top;
        width: 100%;
        height: 1;
        background: $background;
        padding: 0 1;
    }
    /* Equal-width sides (1fr) with an auto-width center keep the breadcrumb at
       the bar's true center regardless of how long the server / user labels are. */
    TopBar .topbar-server {
        width: 1fr;
        padding: 0 1;
        color: $text-muted;
    }
    TopBar .topbar-crumb {
        width: auto;
        text-align: center;
        text-style: bold;
        color: $text;
    }
    TopBar .topbar-user {
        width: 1fr;
        text-align: right;
        padding: 0 1;
        color: $text-muted;
    }
    """

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self, breadcrumb: str = "") -> None:
        super().__init__()
        self._breadcrumb = breadcrumb

    def compose(self) -> ComposeResult:
        yield Label(self._server_label(), classes="topbar-server")
        yield Label(self._breadcrumb, classes="topbar-crumb")
        yield Label(self._user_label(), classes="topbar-user")

    def _server_label(self) -> Text:
        info = self.app.system_info
        name = info.server_name if info is not None else None
        domain = self._server_domain()
        label = Text(name or domain or "tissue")
        # Append the domain alongside the name so the user can still tell which
        # instance they are on.
        if name and domain:
            label.append(f"  ·  {domain}", style="dim")
        return label

    def _server_domain(self) -> str:
        client = self.app.client
        if client is None:
            return ""
        return urlparse(client.host).netloc or client.host

    def _user_label(self) -> str:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        if profile is None:
            return ""
        return profile.username or profile.email or ""
