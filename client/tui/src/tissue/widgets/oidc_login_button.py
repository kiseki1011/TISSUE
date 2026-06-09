from pathlib import Path
from typing import Any

from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.widgets import Button, Label

from tissue.rendering.icon import TGP_AVAILABLE, make_icon_widget

_IDP_ASSET_DIR = Path(__file__).parent.parent / "assets" / "idp"


class OidcLoginButton(Button):
    """Login button for OIDC mode. A provider logo on the left, label on the right.

    The logo is picked from the bundled IdP assets by `icon_key` for the current theme,
    falling back to a generic icon, then to text only.
    """

    DEFAULT_CSS = """
    OidcLoginButton {
        width: 100%;
        height: 3;
        padding: 0;
        align: center middle;
    }
    OidcLoginButton .oidc-btn-row {
        width: auto;
        height: 100%;
        align: center middle;
    }
    OidcLoginButton .oidc-btn-icon {
        width: 2;
        height: 1;
        margin-right: 1;
    }
    OidcLoginButton .oidc-btn-label {
        width: auto;
        height: auto;
        content-align: center middle;
    }
    OidcLoginButton:focus .oidc-btn-label {
        color: $accent;
        text-style: bold;
    }
    """

    def __init__(
        self, label_text: str, icon_key: str | None = None, **kwargs: Any
    ) -> None:
        super().__init__(label="", **kwargs)
        self.border_title = "SSO"
        self._label_text = label_text
        key = (icon_key or "").strip().lower()
        if key and not all(c.isalnum() or c in "-_" for c in key):
            key = ""
        self._icon_key = key

    def compose(self) -> ComposeResult:
        with Horizontal(classes="oidc-btn-row"):
            # Only render the logo on terminals with the Kitty graphics protocol (TGP)
            # Half-block fallback looks horrbile here!!
            icon_path = self._icon_path() if TGP_AVAILABLE else None
            if icon_path is not None:
                icon = make_icon_widget(icon_path)
                icon.add_class("oidc-btn-icon")
                yield icon
            yield Label(self._label_text, classes="oidc-btn-label")

    def _icon_path(self) -> Path | None:
        suffix = "_dark" if self._is_dark_theme() else "_light"
        for key in (self._icon_key, "generic"):
            if not key:
                continue
            path = _IDP_ASSET_DIR / f"{key}{suffix}.png"
            if path.is_file():
                return path
        return None

    def _is_dark_theme(self) -> bool:
        theme = getattr(self.app, "current_theme", None)
        if theme is None:
            return True
        return bool(getattr(theme, "dark", True))
