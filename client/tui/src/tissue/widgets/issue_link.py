from __future__ import annotations

from typing import TYPE_CHECKING

from textual import events
from textual.containers import Horizontal
from textual.message import Message
from textual.widgets import Static

if TYPE_CHECKING:
    from rich.text import Text


class IssueLink(Static):
    """An issue key (plus its type label) rendered as plain, left-aligned, clickable
    text — a link, not a button. Clicking posts `IssueLink.Clicked(issue_key)`, which
    bubbles to the screen to open that issue's detail. `markup=False` keeps a title
    containing '[' literal (no markup parsing / crash); a pre-built `Text` label
    (e.g. the key plus a colour-tinted type) is rendered as-is."""

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

    def __init__(self, issue_key: str, label: str | Text, **kwargs: object) -> None:
        super().__init__(label, markup=False, **kwargs)  # type: ignore[arg-type]
        self.issue_key = issue_key

    def on_click(self, event: events.Click) -> None:
        event.stop()
        self.post_message(self.Clicked(self.issue_key))


class IssueRefRow(Horizontal):
    """One related-issue row — a clickable issue-key link (its type label tinted by
    the type's colour) on the left, a status chip on the right, and an optional ✕
    remove button after it. Self-styled (scoped DEFAULT_CSS) so it lays out the same
    in the dashboard read view and the project hub without per-screen CSS."""

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
