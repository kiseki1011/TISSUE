from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.widgets import Label

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TopBar(Horizontal):
    """Persistent top bar.

        server (left) | breadcrumb (center) | user (right).

    On every PostAuthScreen to provide information of identity/location.
    It tells the user where they are (breadcrumb) and who they are (username).
    """

    DEFAULT_CSS = """
    TopBar {
        dock: top;
        width: 100%;
        height: 1;
        background: $primary 20%;
    }
    TopBar .topbar-server {
        width: auto;
        max-width: 45%;
        padding: 0 1;
        color: $text-muted;
    }
    TopBar .topbar-crumb {
        width: 1fr;
        text-align: center;
        text-style: bold;
        color: $text;
    }
    TopBar .topbar-user {
        width: auto;
        max-width: 30%;
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

    def _server_label(self) -> str:
        info = self.app.system_info
        name = info.server_name if info is not None else None
        client = self.app.client
        host = client.host if client is not None else ""
        return name or host or "tissue"

    def _user_label(self) -> str:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        if profile is None:
            return ""
        return profile.username or profile.email or ""
