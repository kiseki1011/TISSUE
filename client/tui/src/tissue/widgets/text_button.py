from textual.widgets import Button


class TextButton(Button):
    """A Button rendered as plain colored text, with no border or background."""

    DEFAULT_CSS = """
    TextButton {
        border: none;
        background: transparent;
        color: $primary;
        min-width: 0;
        height: 1;
        padding: 0 0;
    }

    TextButton:hover {
        border: none;
        background: transparent;
        color: $accent;
    }

    TextButton:focus {
        border: none;
        background: transparent;
        color: $background;
        text-style: bold;
    }
    """
