from textual import events
from textual.message import Message
from textual.widget import Widget

_BACK_BTN_SHAPE = "[←]"


class BackButton(Widget, can_focus=True):
    DEFAULT_CSS = """
    BackButton {
        width: 3;
        height: 1;
        background: transparent;
        color: $primary;
        text-style: bold;
        content-align: center middle;
    }

    BackButton:focus {
        color: $accent;
        text-style: bold;
    }
    """

    class Pressed(Message):
        def __init__(self, button: BackButton) -> None:
            super().__init__()
            self.button = button

        @property
        def control(self) -> BackButton:
            return self.button

    def render(self) -> str:
        return _BACK_BTN_SHAPE

    def on_click(self) -> None:
        self.focus()
        self.post_message(self.Pressed(self))

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Pressed(self))
            event.stop()
