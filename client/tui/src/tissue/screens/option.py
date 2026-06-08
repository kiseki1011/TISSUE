from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.css.query import NoMatches
from textual.theme import BUILTIN_THEMES
from textual.widget import Widget
from textual.widgets import Label, TabbedContent, TabPane, Tabs

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
        Binding("h", "tab_prev", show=False, priority=True),
        Binding("l", "tab_next", show=False, priority=True),
    ]

    def __init__(self, config_manager: ConfigManager) -> None:
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        settings = self.config_manager.settings

        with Container(id="option-modal-dialog", classes="dialog"):
            with TabbedContent(initial="settings-tab"):
                with TabPane(i18n.get("option_tab_settings"), id="settings-tab"):
                    yield OptionPicker(
                        label=i18n.get("option_theme"),
                        options=[
                            (name, name) for name in sorted(BUILTIN_THEMES.keys())
                        ],
                        current_value=settings.theme,
                        id="theme_picker",
                    )
                    yield OptionPicker(
                        label=i18n.get("option_border_style"),
                        options=[
                            (style, i18n.get(f"option_border_{style}"))
                            for style in self.app.BORDER_STYLES
                        ],
                        current_value=settings.border_style,
                        id="border_picker",
                    )
                    yield OptionPicker(
                        label=i18n.get("option_language"),
                        options=i18n.language_options(),
                        current_value=settings.language,
                        id="language_picker",
                    )
                with TabPane(i18n.get("option_tab_info"), id="info-tab"):
                    yield from self._build_info_widgets()

    def on_mount(self) -> None:
        dialog = self.query_one("#option-modal-dialog", Container)
        dialog.border_title = i18n.get("option_border_title")
        dialog.border_subtitle = i18n.get("option_border_subtitle")
        self.query_one("#theme_picker", OptionPicker).focus()

    def _build_info_widgets(self) -> list[Widget]:
        return [
            self._server_section(),
            self._session_section(),
        ]

    def _server_section(self) -> Container:
        server_url = self.config_manager.state.current_server_url or i18n.get(
            "option_info_not_connected"
        )
        lines: list[Widget] = [
            Label(
                i18n.get("option_info_section_server"),
                classes="info-section-header",
            ),
            Label(
                f"{i18n.get('option_info_url')}: {server_url}",
                classes="info-line",
            ),
        ]
        # Server fields only rendered after successful ping
        info = self.app.system_info
        if info is not None:
            lines.append(
                Label(
                    f"{i18n.get('option_info_name')}: {info.server_name or '-'}",
                    classes="info-line",
                )
            )
            lines.append(
                Label(
                    f"{i18n.get('option_info_version')}: {info.version or '-'}",
                    classes="info-line",
                )
            )
        return Container(*lines, classes="info-section")

    def _session_section(self) -> Container:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        info = self.app.system_info
        email_required = bool(info and info.setup and info.setup.email_required)

        children: list[Widget] = [
            Label(
                i18n.get("option_info_section_session"),
                classes="info-section-header",
            ),
        ]

        if profile is not None and profile.email:
            children.append(
                Label(
                    f"{i18n.get('option_info_email')}: {profile.email}",
                    classes="info-line",
                )
            )
        # username is the identifier when email is not required
        if not email_required and profile is not None and profile.username:
            children.append(
                Label(
                    f"{i18n.get('option_info_username')}: {profile.username}",
                    classes="info-line",
                )
            )
        return Container(*children, classes="info-section")

    def action_close(self) -> None:
        self.dismiss(None)

    def action_nav_up(self) -> None:
        focused = self.focused

        # First picker → jump to tab headers
        if isinstance(focused, OptionPicker):
            pickers = self._pickers_in_active_pane()
            if pickers and pickers[0] is focused:
                tabs = self._tabs_widget()
                if tabs is not None:
                    tabs.focus()
                    return
        # Tab headers → jump to last picker in active pane
        if isinstance(focused, Tabs):
            pickers = self._pickers_in_active_pane()
            if pickers:
                pickers[-1].focus()
                return
        self.focus_previous()

    def action_nav_down(self) -> None:
        focused = self.focused

        # Tab headers → jump to first picker in active pane
        if isinstance(focused, Tabs):
            pickers = self._pickers_in_active_pane()
            if pickers:
                pickers[0].focus()
                return
        # Last picker → jump to tab headers
        if isinstance(focused, OptionPicker):
            pickers = self._pickers_in_active_pane()
            if pickers and pickers[-1] is focused:
                tabs = self._tabs_widget()
                if tabs is not None:
                    tabs.focus()
                    return
        self.focus_next()

    def action_tab_prev(self) -> None:
        tabs = self._tabs_widget()
        if tabs is not None:
            tabs.action_previous_tab()

    def action_tab_next(self) -> None:
        tabs = self._tabs_widget()
        if tabs is not None:
            tabs.action_next_tab()

    def _pickers_in_active_pane(self) -> list[OptionPicker]:
        tabs = self.query_one(TabbedContent)
        try:
            pane = tabs.query_one(f"TabPane#{tabs.active}", TabPane)
        except NoMatches:
            return []
        return list(pane.query(OptionPicker))

    def _tabs_widget(self) -> Tabs | None:
        try:
            return self.query_one(TabbedContent).query_one(Tabs)
        except NoMatches:
            return None

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        focused = self.focused
        if action in ("tab_prev", "tab_next"):
            return isinstance(focused, Tabs)
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
