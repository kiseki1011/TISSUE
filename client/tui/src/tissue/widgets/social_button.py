from pathlib import Path
from typing import Any

from textual.app import ComposeResult
from textual.widgets import Button

from tissue.rendering.icon import make_icon_widget

_ASSET_DIR = Path(__file__).parent.parent / "assets" / "social"

_PROVIDER_CONFIG: dict[str, dict[str, str]] = {
    "GITHUB": {
        "label": "GitHub",
        "image_dark": "github-invertocat-white.png",
        "image_light": "github-invertocat-black.png",
    },
    "GOOGLE": {
        "label": "Google",
        "image_dark": "google-g.png",
        "image_light": "google-g.png",
    },
    "GITLAB": {
        "label": "GitLab",
        "image_dark": "gitlab-rgb.png",
        "image_light": "gitlab-rgb.png",
    },
}


class SocialButton(Button):
    DEFAULT_CSS = """
    SocialButton {
        width: 1fr;
        height: 3;
        background: transparent;
        background-tint: transparent 0%;
        padding: 0;
        align: center middle;
        border-title-align: center;
    }

    SocialButton .-social-icon {
        width: 2;
        height: 1;
    }
    """

    def __init__(self, provider: str, **kwargs: Any) -> None:
        self.provider = provider.upper()
        self._config = _PROVIDER_CONFIG.get(
            self.provider, {"label": self.provider.title()}
        )
        label_text = self._config.get("label", self.provider.title())
        super().__init__(label="", classes="-btn-secondary", **kwargs)
        self.border_title = label_text
        self.tooltip = f"Continue with {label_text}"

    def compose(self) -> ComposeResult:
        asset = self._pick_asset()
        if not asset:
            return
        icon = make_icon_widget(_ASSET_DIR / asset)
        icon.add_class("-social-icon")
        yield icon

    def _pick_asset(self) -> str:
        dark = self._is_dark_theme()
        key = "image_dark" if dark else "image_light"
        return self._config.get(key, "")

    def _is_dark_theme(self) -> bool:
        theme = getattr(self.app, "current_theme", None)
        if theme is None:
            return True
        return bool(getattr(theme, "dark", True))
