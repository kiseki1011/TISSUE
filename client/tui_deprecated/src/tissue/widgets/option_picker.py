from pathlib import Path
from typing import Any

from textual import events
from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.message import Message
from textual.widget import Widget
from textual.widgets import Label

_CSS_PATH = Path(__file__).parent / "css" / "option_picker.tcss"


class OptionPicker(Widget, can_focus=True):
    DEFAULT_CSS = _CSS_PATH.read_text()

    class Changed(Message):
        def __init__(self, picker: "OptionPicker", value: Any) -> None:
            super().__init__()
            self.picker = picker
            self.value = value

        @property
        def control(self) -> "OptionPicker":
            return self.picker

    def __init__(
        self,
        label: str,
        options: list[tuple[Any, str]],
        current_value: Any,
        **kwargs,
    ):
        super().__init__(**kwargs)
        self._label = label
        self._options = options
        self._index = next(
            (i for i, (v, _) in enumerate(options) if v == current_value), 0
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

    def update_label(self, label: str) -> None:
        self._label = label
        self.query_one(".picker-label", Label).update(label)

    def update_options(
        self, options: list[tuple[Any, str]], current_value: Any
    ) -> None:
        self._options = options
        self._index = next(
            (i for i, (v, _) in enumerate(options) if v == current_value), 0
        )
        self.query_one(".picker-value", Label).update(self._current_display())

    def _current_display(self) -> str:
        return self._options[self._index][1]

    def on_key(self, event: events.Key) -> None:
        if event.key == "left":
            event.stop()
            self._cycle(-1)
        elif event.key == "right":
            event.stop()
            self._cycle(1)

    def _cycle(self, direction: int) -> None:
        if not self._options:
            return
        self._index = (self._index + direction) % len(self._options)
        self.query_one(".picker-value", Label).update(self._current_display())
        self.post_message(self.Changed(self, self.value))
