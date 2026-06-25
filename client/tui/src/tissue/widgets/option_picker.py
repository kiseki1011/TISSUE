from typing import Any

from textual import events
from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.message import Message
from textual.widget import Widget
from textual.widgets import Label


class OptionPicker(Widget, can_focus=True):
    DEFAULT_CSS = """
    OptionPicker {
        height: 5;
        width: 100%;
        padding: 1 0 0 0;
    }

    OptionPicker > .picker-label {
        color: $text-muted;
        height: 1;
        width: 100%;
        text-align: center;
        margin-bottom: 1;
        padding: 0;
    }

    OptionPicker > .picker-row {
        height: 1;
        width: 100%;
    }

    OptionPicker .picker-arrow {
        width: 3;
        text-align: center;
        color: $text-muted;
    }

    OptionPicker .picker-value {
        width: 1fr;
        text-align: center;
        color: $foreground;
    }

    OptionPicker:focus {
        background: $primary 20%;
    }

    OptionPicker:focus > .picker-label {
        color: $accent;
        text-style: bold;
    }

    OptionPicker:focus .picker-arrow {
        color: $accent;
        text-style: bold;
    }

    OptionPicker:focus .picker-value {
        color: $accent;
        text-style: bold;
    }
    """

    class Changed(Message):
        def __init__(self, picker: OptionPicker, value: Any) -> None:
            super().__init__()
            self.picker = picker
            self.value = value

        @property
        def control(self) -> OptionPicker:
            return self.picker

    def __init__(
        self,
        label: str,
        options: list[tuple[Any, str]],
        current_value: Any,
        **kwargs: Any,
    ) -> None:
        super().__init__(**kwargs)
        self._label = label
        self._options = options
        self._index = next(
            (
                index
                for index, (value, _) in enumerate(options)
                if value == current_value
            ),
            0,
        )

    def compose(self) -> ComposeResult:
        yield Label(self._label, classes="picker-label")
        yield Horizontal(
            Label("◀", classes="picker-arrow picker-arrow-left"),
            Label(self._current_display(), classes="picker-value"),
            Label("▶", classes="picker-arrow picker-arrow-right"),
            classes="picker-row",
        )

    @property
    def value(self) -> Any:
        return self._options[self._index][0]

    def _current_display(self) -> str:
        return self._options[self._index][1]

    def on_key(self, event: events.Key) -> None:
        if event.key in ("left", "h"):
            event.stop()
            self._cycle(-1)
        elif event.key in ("right", "l"):
            event.stop()
            self._cycle(1)

    def _cycle(self, direction: int) -> None:
        if not self._options:
            return
        self._index = (self._index + direction) % len(self._options)
        self.query_one(".picker-value", Label).update(self._current_display())
        self.post_message(self.Changed(self, self.value))
