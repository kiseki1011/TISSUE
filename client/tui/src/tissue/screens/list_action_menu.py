from textual import events, on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.screen import ModalScreen
from textual.widgets import OptionList
from textual.widgets.option_list import Option

from tissue.i18n.manager import i18n


class ListActionMenu(ModalScreen[str | None]):
    CSS_PATH = "css/list_action_menu.tcss"

    BINDINGS = [
        Binding("escape", "close", show=False),
    ]

    def __init__(
        self,
        options: list[Option],
        anchor_x: int = 0,
        anchor_y: int = 0,
    ):
        super().__init__()
        self.options = options
        self.anchor_x = anchor_x
        self.anchor_y = anchor_y

    def compose(self) -> ComposeResult:
        yield Container(
            OptionList(*self.options, id="action-menu"),
            id="action-menu-dialog",
        )

    def on_mount(self) -> None:
        dialog = self.query_one("#action-menu-dialog", Container)
        dialog.border_title = i18n.get("actions_title")
        dialog.styles.offset = (self.anchor_x, self.anchor_y)
        self.query_one("#action-menu", OptionList).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    def on_click(self, event: events.Click) -> None:
        if event.widget is self:
            self.dismiss(None)

    @on(OptionList.OptionSelected, "#action-menu")
    def on_selected(self, event: OptionList.OptionSelected) -> None:
        self.dismiss(event.option.id)
