"""Reusable search/action row.

A bordered search `Input` (border title "Search") plus an optional slot of
trailing controls (buttons/toggles). Gives every tab the same spacing so
Projects / Wiki / (future) Issues read consistently.

It is presentational: the search `Input.Submitted` and any control's
`Button.Pressed` bubble to the host screen, which handles them by id. Pass a
distinct `input_id` per use so the host can target the right search box.
"""

from textual.app import ComposeResult
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Input


class SearchBar(Horizontal):
    DEFAULT_CSS = """
    SearchBar {
        width: 100%;
        height: auto;
        /* No top padding: the bar sits flush against whatever is above it (a
           tab header or a docked sidebar's top). Hosts add a gap below if a
           content pane should keep its distance. */
        padding: 0 2 0 2;
        align-vertical: middle;
    }
    SearchBar .search-bar-input {
        width: 1fr;
        background: transparent;
    }
    SearchBar Button {
        margin-left: 2;
        width: auto;
    }
    """

    def __init__(
        self,
        *controls: Widget,
        input_id: str = "search-input",
        placeholder: str = "Search…",
        value: str = "",
    ) -> None:
        super().__init__()
        self._controls = controls
        self._input_id = input_id
        self._placeholder = placeholder
        self._value = value

    def compose(self) -> ComposeResult:
        search = Input(
            value=self._value,
            placeholder=self._placeholder,
            id=self._input_id,
            classes="search-bar-input",
        )
        search.border_title = "Search"
        yield search
        yield from self._controls
