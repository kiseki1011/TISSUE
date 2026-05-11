from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.theme import BUILTIN_THEMES

from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal
from tissue.widgets.option_picker import OptionPicker


class OptionModal(TissueModal[None]):
    CSS_PATH = "option.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
        Binding("ctrl+o", "close", show=False),
        Binding("up", "nav_up", show=False, priority=True),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("k", "nav_up", show=False, priority=True),
        Binding("j", "nav_down", show=False, priority=True),
    ]

    def __init__(self, config_manager: ConfigManager) -> None:
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        settings = self.config_manager.settings

        dialog = Container(
            OptionPicker(
                label=i18n.get("option_language"),
                options=i18n.language_options(),
                current_value=settings.language,
                id="language_picker",
            ),
            OptionPicker(
                label=i18n.get("option_theme"),
                options=[(name, name) for name in sorted(BUILTIN_THEMES.keys())],
                current_value=settings.theme,
                id="theme_picker",
            ),
            OptionPicker(
                label=i18n.get("option_border_style"),
                options=[
                    (style, i18n.get(f"option_border_{style}"))
                    for style in self.app.BORDER_STYLES
                ],
                current_value=settings.border_style,
                id="border_picker",
            ),
            id="option-modal-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("option_border_title")
        dialog.border_subtitle = i18n.get("option_border_subtitle")
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#language_picker", OptionPicker).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    def action_nav_up(self) -> None:
        self.focus_previous()

    def action_nav_down(self) -> None:
        self.focus_next()

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        if action in ("nav_up", "nav_down"):
            focused = self.focused
            if not isinstance(focused, OptionPicker):
                return False
        return True

    @on(OptionPicker.Changed, "#language_picker")
    def on_language_changed(self, event: OptionPicker.Changed) -> None:
        self.app.change_language(event.value)

    @on(OptionPicker.Changed, "#theme_picker")
    def on_theme_changed(self, event: OptionPicker.Changed) -> None:
        self.app.change_theme(event.value)

    @on(OptionPicker.Changed, "#border_picker")
    def on_border_changed(self, event: OptionPicker.Changed) -> None:
        self.app.change_border_style(event.value)
