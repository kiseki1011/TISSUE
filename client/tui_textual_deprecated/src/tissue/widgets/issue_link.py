from __future__ import annotations

from typing import TYPE_CHECKING

from textual import events
from textual.containers import Horizontal
from textual.message import Message
from textual.widgets import Static

if TYPE_CHECKING:
    from rich.text import Text


class IssueLink(Static):
    """A clickable issue-key link that posts `IssueLink.Clicked` to open the issue.

    `markup=False` so a title containing a literal '[' renders as-is instead of
    being parsed as markup and crashing.
    """

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
        """Posted when an issue link is clicked, carrying the issue key to open."""

        def __init__(self, issue_key: str) -> None:
            super().__init__()
            self.issue_key = issue_key

    def __init__(self, issue_key: str, label: str | Text, **kwargs: object) -> None:
        super().__init__(label, markup=False, **kwargs)  # type: ignore[arg-type]
        self.issue_key = issue_key

    def on_click(self, event: events.Click) -> None:
        event.stop()
        self.post_message(self.Clicked(self.issue_key))


class IssueRefRow(Horizontal):
    """One related-issue row holding a link, status chip, and optional remove button.

    Self-styled with scoped `DEFAULT_CSS` so it lays out the same in the dashboard
    read view and the project hub without per-screen CSS.
    """

    DEFAULT_CSS = """
    IssueRefRow {
        width: 1fr;
        height: 1;
    }
    IssueRefRow .iref-rel-label {
        width: 17;
        color: $text-muted;
    }
    IssueRefRow .iref-status {
        width: auto;
        margin: 0 0 0 1;
    }
    """
