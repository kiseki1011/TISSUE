from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.screen import ModalScreen

from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.widgets.i18n_widgets import I18nContainer
from tissue.widgets.option_picker import OptionPicker


class OptionModal(ModalScreen[None]):
    CSS_PATH = "css/option.tcss"

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
        config = self.config_manager.get_config()

        yield I18nContainer(
            OptionPicker(
                label=i18n.get("option_language"),
                options=i18n.language_options(),
                current_value=config.language,
                id="language_picker",
            ),
            OptionPicker(
                label=i18n.get("option_theme"),
                options=self.app.theme_options,
                current_value=config.theme,
                id="theme_picker",
            ),
            OptionPicker(
                label=i18n.get("option_vim_keybindings"),
                options=[
                    (False, i18n.get("option_off")),
                    (True, i18n.get("option_on")),
                ],
                current_value=config.vim_keybindings,
                id="vim_picker",
            ),
            id="option-modal-dialog",
            title_key="option_border_title",
            subtitle_key="option_border_subtitle",
        )

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh_pickers)
        self.query_one("#language_picker", OptionPicker).focus()

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_pickers)

    def _refresh_pickers(self) -> None:
        self.query_one("#language_picker", OptionPicker).update_label(
            i18n.get("option_language")
        )
        self.query_one("#theme_picker", OptionPicker).update_label(
            i18n.get("option_theme")
        )
        vim_picker = self.query_one("#vim_picker", OptionPicker)
        vim_picker.update_label(i18n.get("option_vim_keybindings"))
        config = self.config_manager.get_config()
        vim_picker.update_options(
            [
                (False, i18n.get("option_off")),
                (True, i18n.get("option_on")),
            ],
            current_value=config.vim_keybindings,
        )

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
        self.config_manager.save_settings(language=event.value)
        i18n.set_language(event.value)

    @on(OptionPicker.Changed, "#theme_picker")
    def on_theme_changed(self, event: OptionPicker.Changed) -> None:
        self.config_manager.save_settings(theme=event.value)
        self.app.theme = event.value

    @on(OptionPicker.Changed, "#vim_picker")
    def on_vim_changed(self, event: OptionPicker.Changed) -> None:
        self.config_manager.save_settings(vim_keybindings=event.value)
        self._refresh_pickers()
