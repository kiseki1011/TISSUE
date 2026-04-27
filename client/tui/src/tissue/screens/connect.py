import dataclasses
import logging

from textual import events, on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import (
    Button,
    Footer,
    Header,
    Input,
    Label,
    ListItem,
    ListView,
    Static,
)
from textual.widgets.option_list import Option

from tissue.api.auth import AuthAPI
from tissue.api.errors import ApiNetworkError, TissueApiError
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.description_input import DescriptionInputModal
from tissue.screens.list_action_menu import ListActionMenu
from tissue.screens.login import LoginScreen
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput

log = logging.getLogger(__name__)

_FULL_HEIGHT_THRESHOLD = 36
_COMPACT_HEIGHT_THRESHOLD = 28


class _UrlListItem(ListItem):
    def __init__(self, url: str, kind: str, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.url = url
        self.kind = kind


class ConnectScreen(Screen):
    CSS_PATH = "css/connect.tcss"

    BINDINGS = [
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
        Binding("b", "bookmark_focused", "bookmark"),
        Binding("r", "rename_description_focused", "description"),
        Binding("d", "delete_focused", "delete"),
        Binding("m", "menu_focused", "menu"),
    ]

    def __init__(self, config_manager: ConfigManager):
        super().__init__()
        self.config_manager = config_manager
        labels = {
            "b": i18n.get("binding_bookmark"),
            "r": i18n.get("binding_rename_description"),
            "d": i18n.get("binding_delete"),
            "m": i18n.get("binding_menu"),
        }
        for key, label in labels.items():
            existing = self._bindings.key_to_bindings.get(key, [])
            self._bindings.key_to_bindings[key] = [
                dataclasses.replace(b, description=label) for b in existing
            ]

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Horizontal(
                ModalInput(
                    placeholder=i18n.get("server_url_placeholder"),
                    id="server_url_input",
                ),
                BracketButton(i18n.get("connect_btn"), id="connect_btn"),
                classes="input-row",
            ),
            ListView(id="bookmark_list"),
            ListView(id="history_list"),
            id="dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog = self.query_one("#dialog", Container)
        dialog.border_title = i18n.get("connect_server_border_title")

        server_url_input = self.query_one("#server_url_input", ModalInput)
        server_url_input.border_title = i18n.get("server_url_title")

        self.query_one("#bookmark_list", ListView).border_title = i18n.get(
            "bookmark_list_title"
        )
        self.query_one("#history_list", ListView).border_title = i18n.get(
            "history_list_title"
        )

        self.query_one("#server_url_input", ModalInput).focus()
        self.update_bookmarks()
        self.update_history()

    def on_screen_resume(self) -> None:
        self.query_one("#server_url_input", ModalInput).focus()
        self.update_bookmarks()
        self.update_history()

    def on_resize(self, event: events.Resize) -> None:
        self._apply_compact_mode()

    def _apply_compact_mode(self) -> None:
        dialog = self.query_one("#dialog", Container)
        h = self.size.height
        if h < _COMPACT_HEIGHT_THRESHOLD:
            dialog.add_class("-compact")
            dialog.remove_class("-auto")
        elif h < _FULL_HEIGHT_THRESHOLD:
            dialog.add_class("-auto")
            dialog.remove_class("-compact")
        else:
            dialog.remove_class("-auto")
            dialog.remove_class("-compact")

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        list_actions = (
            "bookmark_focused",
            "rename_description_focused",
            "delete_focused",
            "menu_focused",
        )
        if action not in list_actions:
            return True
        focused = self.focused
        if not isinstance(focused, ListView):
            return False
        if action == "bookmark_focused" and focused.id != "history_list":
            return False
        if action == "rename_description_focused" and focused.id != "bookmark_list":
            return False
        if focused.index is None:
            return None
        return True

    def action_nav_down(self) -> None:
        focused = self.focused
        if isinstance(focused, ListView):
            if len(focused.children) == 0:
                self.focus_next()
                return
            if focused.index is None:
                focused.index = 0
                return
            if focused.index >= len(focused.children) - 1:
                focused.index = None
                self.focus_next()
                return
            focused.index += 1
            return
        self.focus_next()

    def action_nav_up(self) -> None:
        focused = self.focused
        if isinstance(focused, ListView):
            if len(focused.children) == 0:
                self.focus_previous()
                return
            if focused.index is None:
                focused.index = 0
                return
            if focused.index == 0:
                focused.index = None
                self.focus_previous()
                return
            focused.index -= 1
            return
        self.focus_previous()

    def action_bookmark_focused(self) -> None:
        focused = self.focused
        if not isinstance(focused, ListView) or focused.id != "history_list":
            return
        idx = focused.index
        if idx is None:
            return
        history = self.config_manager.get_config().server_history
        if not (0 <= idx < len(history)):
            return
        item = history[idx]
        self._open_description_modal(item.url, item.server_name)

    def action_delete_focused(self) -> None:
        focused = self.focused
        if not isinstance(focused, ListView):
            return
        idx = focused.index
        if idx is None:
            return
        if focused.id == "history_list":
            history = self.config_manager.get_config().server_history
            if 0 <= idx < len(history):
                self.config_manager.remove_history_item(history[idx].url)
                self.update_history()
                self.app.notify(i18n.get("history_removed"), timeout=2)
        elif focused.id == "bookmark_list":
            bookmarks = self.config_manager.get_config().bookmarks
            if 0 <= idx < len(bookmarks):
                self.config_manager.remove_bookmark(bookmarks[idx].url)
                self.update_bookmarks()
                self.app.notify(i18n.get("bookmark_removed"), timeout=2)

    def action_rename_description_focused(self) -> None:
        focused = self.focused
        if not isinstance(focused, ListView) or focused.id != "bookmark_list":
            return
        idx = focused.index
        if idx is None:
            return
        bookmarks = self.config_manager.get_config().bookmarks
        if not (0 <= idx < len(bookmarks)):
            return
        bookmark = bookmarks[idx]
        self._open_description_modal(
            bookmark.url,
            bookmark.server_name,
            default_value=bookmark.description or "",
            success_key="bookmark_description_updated",
        )

    def action_menu_focused(self) -> None:
        focused = self.focused
        if not isinstance(focused, ListView):
            return
        idx = focused.index
        if idx is None:
            return
        item = focused.children[idx]
        if not isinstance(item, _UrlListItem):
            return
        region = item.region
        self._open_action_menu(item.kind, item.url, region.x, region.y)

    def on_click(self, event: events.Click) -> None:
        if event.button != 3:
            return
        widget = event.widget
        while widget is not None:
            if isinstance(widget, _UrlListItem):
                self._open_action_menu(
                    widget.kind,
                    widget.url,
                    event.screen_x,
                    event.screen_y,
                )
                return
            widget = widget.parent

    def on_key(self, event: events.Key) -> None:
        if event.key == "escape":
            focused = self.focused
            if isinstance(focused, ModalInput) and not focused._editing:
                self.set_focus(None)

    @work
    async def _open_action_menu(
        self, kind: str, url: str, x: int = 0, y: int = 0
    ) -> None:
        options: list[Option] = []
        if kind == "history":
            options.append(Option(i18n.get("action_bookmark"), id="bookmark"))
        else:
            options.append(
                Option(i18n.get("action_rename_description"), id="rename_description")
            )
        options.append(Option(i18n.get("action_delete"), id="delete"))

        action = await self.app.push_screen_wait(
            ListActionMenu(options=options, anchor_x=x, anchor_y=y)
        )

        if action == "bookmark":
            history = self.config_manager.get_config().server_history
            entry = next((h for h in history if h.url == url), None)
            server_name = entry.server_name if entry else None
            self._open_description_modal(url, server_name)
        elif action == "rename_description":
            bookmark = next(
                (b for b in self.config_manager.get_config().bookmarks if b.url == url),
                None,
            )
            if bookmark is None:
                return
            self._open_description_modal(
                url,
                bookmark.server_name,
                default_value=bookmark.description or "",
                success_key="bookmark_description_updated",
            )
        elif action == "delete":
            if kind == "history":
                self.config_manager.remove_history_item(url)
                self.update_history()
                self.app.notify(i18n.get("history_removed"), timeout=2)
            else:
                self.config_manager.remove_bookmark(url)
                self.update_bookmarks()
                self.app.notify(i18n.get("bookmark_removed"), timeout=2)

    @work
    async def _open_description_modal(
        self,
        url: str,
        server_name: str | None,
        default_value: str | None = None,
        success_key: str = "bookmark_added",
    ) -> None:
        default = default_value if default_value is not None else (server_name or "")
        description = await self.app.push_screen_wait(
            DescriptionInputModal(default_value=default)
        )
        if description is None:
            return
        self.config_manager.add_bookmark(
            url=url,
            server_name=server_name,
            description=description or None,
        )
        self.update_bookmarks()
        self.app.notify(i18n.get(success_key), timeout=2)

    def update_bookmarks(self) -> None:
        bookmarks = self.config_manager.get_config().bookmarks
        list_view = self.query_one("#bookmark_list", ListView)
        list_view.clear()
        for b in bookmarks:
            display_name = b.description or b.server_name or "—"
            li = _UrlListItem(
                b.url,
                "bookmark",
                Horizontal(
                    Label(b.url, classes="col"),
                    Label(display_name, classes="col"),
                ),
            )
            list_view.append(li)

    def update_history(self) -> None:
        history = self.config_manager.get_config().server_history
        list_view = self.query_one("#history_list", ListView)
        list_view.clear()
        for item in history:
            date_str = item.last_connected.strftime("%Y-%m-%d %H:%M")
            li = _UrlListItem(
                item.url,
                "history",
                Horizontal(
                    Label(item.url, classes="col-url"),
                    Label(date_str, classes="col-date"),
                ),
            )
            list_view.append(li)

    @on(Input.Submitted, "#server_url_input")
    @on(Button.Pressed, "#connect_btn")
    def connect_action(self) -> None:
        url = self.query_one("#server_url_input", ModalInput).value.strip()
        if not url:
            self.app.notify(i18n.get("error_enter_url"), severity="error", timeout=2)
            return
        self._do_connect(url)

    @work(exclusive=True)
    async def _do_connect(self, url: str) -> None:
        self.app.notify(i18n.get("connecting", url=url), timeout=2)
        self.app.client.set_base_url(url)
        try:
            info = await AuthAPI(self.app.client).get_system_info()
        except ApiNetworkError as e:
            log.warning("Connect failed (network) to %s: %s", url, e)
            self.app.notify(
                i18n.get("connect_failed", url=url), severity="error", timeout=3
            )
            return
        except TissueApiError as e:
            log.error("Connect failed (api) to %s: %s", url, e)
            self.app.notify(
                i18n.get("connect_failed", url=url), severity="error", timeout=3
            )
            return

        self.config_manager.save_server(url, info.server_name)
        self.app.notify(i18n.get("connect_success", url=url), timeout=2)
        self.app.push_screen(LoginScreen(info, self.config_manager))

    @on(ListView.Selected)
    def on_list_selected(self, event: ListView.Selected) -> None:
        item = event.item
        if isinstance(item, _UrlListItem):
            self.query_one("#server_url_input", ModalInput).value = item.url
            self.query_one("#server_url_input", ModalInput).focus()
