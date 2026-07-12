from textual.content import Content
from textual.style import Style
from textual.widgets import Checkbox


class FilterCheckbox(Checkbox):
    """Checkbox variant that hides the inner button character in the `OFF` state.

    Textual's ToggleButton draws `BUTTON_INNER` in both `ON` and `OFF` states,
    differing only in color, which looks weak in light themes. Hiding the
    character when `OFF` makes it read as an empty box.
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
