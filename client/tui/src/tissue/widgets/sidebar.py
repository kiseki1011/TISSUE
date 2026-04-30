from pathlib import Path

from textual import events, on
from textual.app import ComposeResult
from textual.containers import Container
from textual.message import Message
from textual.reactive import reactive
from textual.widgets import Label, OptionList
from textual.widgets.option_list import Option

from tissue.i18n.manager import i18n

_CSS_PATH = Path(__file__).parent / "css" / "sidebar.tcss"

_USER_MENU_HEIGHT = 5


class _UserSlot(Container, can_focus=True):
    class Activated(Message):
        pass

    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._name: str | None = None
        self._username: str | None = None

    def compose(self) -> ComposeResult:
        yield Label(i18n.get("loading_profile"), id="user_slot_label")

    def update_user(self, name: str | None, username: str | None) -> None:
        self._name = name
        self._username = username
        lbl = self.query_one("#user_slot_label", Label)
        if name and username:
            lbl.update(f"{name} (@{username})")
        elif name:
            lbl.update(name)
        else:
            lbl.update(i18n.get("loading_profile"))

    def on_click(self, event: events.Click) -> None:
        self.focus()
        self.post_message(self.Activated())
        event.stop()

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Activated())
            event.stop()


class Sidebar(Container):
    DEFAULT_CSS = _CSS_PATH.read_text()

    collapsed: reactive[bool] = reactive(False, layout=True)

    class ItemSelected(Message):
        def __init__(self, panel_id: str) -> None:
            super().__init__()
            self.panel_id = panel_id

    class UserMenuRequested(Message):
        def __init__(self, x: int, y: int) -> None:
            super().__init__()
            self.x = x
            self.y = y

    def compose(self) -> ComposeResult:
        yield OptionList(
            *self._build_options(),
            id="sidebar_items",
        )
        yield _UserSlot(id="sidebar_user_slot")

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh_i18n)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _build_options(self) -> list[Option]:
        return [
            Option(i18n.get("sidebar_workspaces"), id="workspaces_panel"),
            Option(i18n.get("sidebar_invitations"), id="invitations_panel"),
        ]

    def _refresh_i18n(self) -> None:
        opts = self.query_one("#sidebar_items", OptionList)
        current_index = opts.highlighted
        opts.clear_options()
        for opt in self._build_options():
            opts.add_option(opt)
        if current_index is not None and 0 <= current_index < opts.option_count:
            opts.highlighted = current_index
        slot = self.query_one(_UserSlot)
        slot.update_user(slot._name, slot._username)

    def watch_collapsed(self, value: bool) -> None:
        if value:
            self.add_class("-collapsed")
        else:
            self.remove_class("-collapsed")

    def set_user(self, name: str | None, username: str | None) -> None:
        self.query_one(_UserSlot).update_user(name, username)

    @on(OptionList.OptionSelected, "#sidebar_items")
    def _on_option_selected(self, event: OptionList.OptionSelected) -> None:
        if event.option.id:
            self.post_message(self.ItemSelected(event.option.id))
        event.stop()

    @on(_UserSlot.Activated)
    def _on_slot_activated(self, event: _UserSlot.Activated) -> None:
        slot = self.query_one(_UserSlot)
        x = slot.region.x
        y = max(0, slot.region.y - _USER_MENU_HEIGHT)
        self.post_message(self.UserMenuRequested(x, y))
        event.stop()
