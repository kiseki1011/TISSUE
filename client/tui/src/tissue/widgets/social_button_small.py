from pathlib import Path

from textual import events
from textual.app import ComposeResult
from textual.message import Message
from textual.widget import Widget

from tissue.rendering import make_icon_widget

ASSET_DIR = Path(__file__).parent.parent / "assets" / "social"

PROVIDER_CONFIG: dict[str, dict[str, str]] = {
    "GITHUB": {
        "label": "Continue with GitHub",
        "image_dark": "github-invertocat-white.png",
        "image_light": "github-invertocat-black.png",
    },
    "GOOGLE": {
        "label": "Continue with Google",
        "image_dark": "google-g.png",
        "image_light": "google-g.png",
    },
    "GITLAB": {
        "label": "Continue with GitLab",
        "image_dark": "gitlab-rgb.png",
        "image_light": "gitlab-rgb.png",
    },
    "MASTODON": {
        "label": "Continue with Mastodon",
        "image_dark": "mastodon-purple.png",
        "image_light": "mastodon-purple.png",
    },
}

_CSS_PATH = Path(__file__).parent / "css" / "social_button_small.tcss"


class SocialButtonSmall(Widget):
    can_focus = True

    DEFAULT_CSS = _CSS_PATH.read_text()

    class Pressed(Message):
        def __init__(self, provider: str) -> None:
            super().__init__()
            self.provider = provider

    def __init__(self, provider: str, **kwargs) -> None:
        super().__init__(**kwargs)
        self.provider = provider.upper()
        self.config = PROVIDER_CONFIG.get(
            self.provider, {"label": self.provider.title()}
        )
        self.border_title = self._display_name()
        self.tooltip = f"Continue with {self._display_name()}"

    def compose(self) -> ComposeResult:
        icon_widget = make_icon_widget(ASSET_DIR / self._pick_asset())
        icon_widget.add_class("-social-icon")
        yield icon_widget

    def on_click(self) -> None:
        self.focus()
        self.post_message(self.Pressed(self.provider))

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Pressed(self.provider))
            event.stop()

    def _display_name(self) -> str:
        label = self.config.get("label", "")
        prefix = "Continue with "
        if label.startswith(prefix):
            return label[len(prefix) :]
        return self.provider.title()

    def _pick_asset(self) -> str:
        dark = getattr(self.app, "dark", True)
        key = "image_dark" if dark else "image_light"
        return self.config.get(key, "")
