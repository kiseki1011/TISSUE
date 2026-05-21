from tissue.widgets.text_button import TextButton


class SidebarNavButton(TextButton):
    """Full-row variant of TextButton for sidebar navigation.

    Same plain-text rendering as TextButton (no border, transparent
    background, single-line height) but the widget spans its container's
    full width. Focus/hover highlights therefore cover the entire row with
    a faint primary tint instead of just the text width.

    Width-spanning is the only structural difference; everything else flows
    from TextButton.
    """

    DEFAULT_CSS = """
    SidebarNavButton {
        width: 100%;
        padding: 0 2;
        content-align: left middle;
        text-align: left;
    }
    """
