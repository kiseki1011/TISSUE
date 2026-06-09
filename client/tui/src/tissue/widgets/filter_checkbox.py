from textual.content import Content
from textual.style import Style
from textual.widgets import Checkbox


class FilterCheckbox(Checkbox):
    """Checkbox variant that hides the inner button character in the `OFF` state.

    Textual's ToggleButton draws `BUTTON_INNER` ('X') in both `ON` and `OFF` state.
    The only difference is colors. The `OFF` seems weak in light colored themes.
    In this custom widget, we just hide the `BUTTON_INNER` if `OFF` state, so it
    appears like an empty box.
    """

    BUTTON_INNER = "✔"

    @property
    def _button(self) -> Content:
        button_style = self.get_visual_style("toggle--button")
        side_style = Style(
            foreground=button_style.background,
            background=self.background_colors[1],
        )
        inner = self.BUTTON_INNER if self.value else " "
        return Content.assemble(
            (self.BUTTON_LEFT, side_style),
            (inner, button_style),
            (self.BUTTON_RIGHT, side_style),
        )
