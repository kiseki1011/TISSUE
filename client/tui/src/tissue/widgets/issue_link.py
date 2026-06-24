from __future__ import annotations

from textual import events
from textual.message import Message
from textual.widgets import Static


class IssueLink(Static):
    """An issue key (plus its type label) rendered as plain, left-aligned, clickable
    text — a link, not a button. Clicking posts `IssueLink.Clicked(issue_key)`, which
    bubbles to the screen to open that issue's detail. `markup=False` keeps a title
    containing '[' literal (no markup parsing / crash)."""

    DEFAULT_CSS = """
    IssueLink {
        width: 1fr;
        height: 1;
        color: $primary;
    }
    IssueLink:hover {
        color: $accent;
        text-style: underline;
    }
    """

    class Clicked(Message):
        """Posted when an issue link is clicked; carries the issue key to open."""

        def __init__(self, issue_key: str) -> None:
            super().__init__()
            self.issue_key = issue_key

    def __init__(self, issue_key: str, label: str, **kwargs: object) -> None:
        super().__init__(label, markup=False, **kwargs)  # type: ignore[arg-type]
        self.issue_key = issue_key

    def on_click(self, event: events.Click) -> None:
        event.stop()
        self.post_message(self.Clicked(self.issue_key))
