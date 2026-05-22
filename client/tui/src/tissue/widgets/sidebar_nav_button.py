from tissue.widgets.text_button import TextButton


class SidebarNavButton(TextButton):
    DEFAULT_CSS = """
    SidebarNavButton {
        width: 100%;
        padding: 0 1;
        content-align: left middle;
        text-align: left;
    }
    """

    def __init__(
        self,
        label: str,
        *,
        shortcut: str | None = None,
        id: str | None = None,
        classes: str | None = None,
    ) -> None:
        # Escape the bracket so Rich markup renders "[<shortcut>]"
        display = f"\\[{shortcut}] {label}" if shortcut else label
        super().__init__(display, id=id, classes=classes)
