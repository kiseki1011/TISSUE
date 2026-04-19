from textual.widgets import Button


class BracketButton(Button):
    def __init__(self, label: str, **kwargs):
        super().__init__(label, **kwargs)
        self._base_label = label

    def on_focus(self):
        self.label = f"\\[{self._base_label}]"

    def on_blur(self):
        self.label = self._base_label
