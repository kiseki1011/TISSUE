from textual.widgets import Button


class BracketButton(Button):
    def __init__(self, label: str, **kwargs):
        super().__init__(label, **kwargs)
        self._base_label = label

    @property
    def base_label(self) -> str:
        return self._base_label

    @base_label.setter
    def base_label(self, value: str) -> None:
        self._base_label = value
        self.label = f"\\[{value}]" if self.has_focus else value

    def on_focus(self):
        self.label = f"\\[{self._base_label}]"

    def on_blur(self):
        self.label = self._base_label
