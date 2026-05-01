from pathlib import Path

from textual import events, on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal, Vertical
from textual.message import Message
from textual.reactive import reactive
from textual.widgets import Label, ListItem, ListView, Static
from textual_image.widget import Image

from tissue.i18n.manager import i18n

_CSS_PATH = Path(__file__).parent / "css" / "sidebar.tcss"
_PROFILE_PLACEHOLDER = (
    Path(__file__).parent.parent / "assets" / "profile_placeholder.png"
)


class _CollapseToggle(Container, can_focus=False):
    class Toggled(Message):
        pass

    def compose(self) -> ComposeResult:
        yield Label("◀┃", id="toggle_icon")

    def update_icon(self, collapsed: bool) -> None:
        self.query_one("#toggle_icon", Label).update("┃▶" if collapsed else "◀┃")

    def on_click(self, event: events.Click) -> None:
        self.post_message(self.Toggled())
        event.stop()

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Toggled())
            event.stop()


class _UserSlot(Container, can_focus=True):
    class Activated(Message):
        pass

    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._name: str | None = None
        self._username: str | None = None
        self._email: str | None = None

    def compose(self) -> ComposeResult:
        with Horizontal(classes="user-top"):
            yield Image(str(_PROFILE_PLACEHOLDER), id="user_avatar")
            with Vertical(id="user_text"):
                yield Label(i18n.get("loading_profile"), id="user_name")
                yield Label("", id="user_handle")
        yield Label("-", id="user_email")

    def update_user(
        self,
        name: str | None,
        username: str | None,
        email: str | None = None,
    ) -> None:
        self._name = name
        self._username = username
        self._email = email
        name_lbl = self.query_one("#user_name", Label)
        handle_lbl = self.query_one("#user_handle", Label)
        email_lbl = self.query_one("#user_email", Label)
        if name and username:
            name_lbl.update(name)
            handle_lbl.update(f"@{username}")
        elif name:
            name_lbl.update(name)
            handle_lbl.update("")
        else:
            name_lbl.update(i18n.get("loading_profile"))
            handle_lbl.update("")
        email_lbl.update(email if email else "-")

    def on_click(self, event: events.Click) -> None:
        self.focus()
        self.post_message(self.Activated())
        event.stop()

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Activated())
            event.stop()


class _LogoutRow(Static, can_focus=True):
    class Pressed(Message):
        pass

    def __init__(self, **kwargs) -> None:
        super().__init__("", **kwargs)
        self.border_title = "(^l)"

    def on_mount(self) -> None:
        self._refresh()
        i18n.subscribe(self._refresh)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh)

    def _refresh(self) -> None:
        self.update(f"⏻  {i18n.get('sidebar_logout')}")

    def on_click(self, event: events.Click) -> None:
        self.post_message(self.Pressed())
        event.stop()

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Pressed())
            event.stop()


class _NavItem(ListItem):
    _ICON_SELECTED = "●"
    _ICON_NORMAL = "○"

    def __init__(
        self,
        panel_id: str,
        i18n_key: str,
        shortcut: str | None = None,
    ) -> None:
        self._i18n_key = i18n_key
        self._shortcut = shortcut
        self._is_selected = False
        self._label = Label("")
        super().__init__(self._label)
        self.panel_id = panel_id
        if shortcut:
            self.border_title = f"({shortcut})"
        self._update_label()

    def _update_label(self) -> None:
        icon = self._ICON_SELECTED if self._is_selected else self._ICON_NORMAL
        text = i18n.get(self._i18n_key)
        self._label.update(f"{icon} {text}")

    def on_mount(self) -> None:
        i18n.subscribe(self._update_label)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._update_label)

    def set_selected(self, value: bool) -> None:
        if value == self._is_selected:
            return
        self._is_selected = value
        self._update_label()


class Sidebar(Container):
    DEFAULT_CSS = _CSS_PATH.read_text()

    collapsed: reactive[bool] = reactive(False, layout=True)

    class ItemSelected(Message):
        def __init__(self, panel_id: str) -> None:
            super().__init__()
            self.panel_id = panel_id

    class LogoutRequested(Message):
        pass

    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._selected_panel_id: str = "workspaces_panel"

    def compose(self) -> ComposeResult:
        yield _CollapseToggle(id="sidebar_toggle")
        with Container(id="sidebar_inner"):
            yield _UserSlot(id="sidebar_user_slot")
            yield ListView(
                _NavItem("workspaces_panel", "sidebar_workspaces", shortcut="1"),
                _NavItem("invitations_panel", "sidebar_invitations", shortcut="2"),
                _NavItem("account_panel", "sidebar_account", shortcut="3"),
                id="sidebar_items",
            )
            yield _LogoutRow(id="sidebar_logout")

    def on_mount(self) -> None:
        self._apply_selected()

    def _apply_selected(self) -> None:
        for item in self.query(_NavItem):
            item.set_selected(item.panel_id == self._selected_panel_id)

    def watch_collapsed(self, value: bool) -> None:
        if value:
            self.add_class("-collapsed")
        else:
            self.remove_class("-collapsed")
        self.query_one(_CollapseToggle).update_icon(value)

    def set_user(
        self,
        name: str | None,
        username: str | None,
        email: str | None = None,
    ) -> None:
        self.query_one(_UserSlot).update_user(name, username, email)

    def select_panel(self, panel_id: str) -> None:
        self._selected_panel_id = panel_id
        self._apply_selected()
        self.post_message(self.ItemSelected(panel_id))

    @on(ListView.Selected, "#sidebar_items")
    def _on_item_selected(self, event: ListView.Selected) -> None:
        if isinstance(event.item, _NavItem):
            self._selected_panel_id = event.item.panel_id
            self._apply_selected()
            self.post_message(self.ItemSelected(event.item.panel_id))
        event.stop()

    @on(_UserSlot.Activated)
    def _on_slot_activated(self, event: _UserSlot.Activated) -> None:
        self._selected_panel_id = "my_profile_panel"
        self._apply_selected()
        self.post_message(self.ItemSelected("my_profile_panel"))
        event.stop()

    @on(_LogoutRow.Pressed)
    def _on_logout_pressed(self, event: _LogoutRow.Pressed) -> None:
        self.post_message(self.LogoutRequested())
        event.stop()

    @on(_CollapseToggle.Toggled)
    def _on_toggle(self, event: _CollapseToggle.Toggled) -> None:
        self.collapsed = not self.collapsed
        event.stop()
