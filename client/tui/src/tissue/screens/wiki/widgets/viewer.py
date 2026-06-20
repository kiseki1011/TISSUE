from __future__ import annotations

from textual.widgets import Markdown, MarkdownViewer
from textual.widgets.markdown import MarkdownTableOfContents


class _WikiViewer(MarkdownViewer):
    """MarkdownViewer adapted for the wiki reader: link clicks bubble up to
    `WikiScreen` for routing, and the outline is forwarded to the sidebar
    (`sidebar_toc`) instead of the viewer's own hidden table of contents.
    """

    # The sidebar outline to feed; set by the screen once it has been mounted.
    sidebar_toc: MarkdownTableOfContents | None = None

    async def _on_markdown_link_clicked(self, message: Markdown.LinkClicked) -> None:
        # prevent_default() stops the stock go() (which treats hrefs as file
        # paths); we do NOT stop the message, so it bubbles to WikiScreen.
        message.prevent_default()

    def _on_markdown_table_of_contents_updated(
        self, message: Markdown.TableOfContentsUpdated
    ) -> None:
        # Forward the outline to the sidebar, then prevent_default() so the stock
        # handler (which rebuilds the viewer's own hidden TOC) is skipped —
        # stop() alone wouldn't suppress it (handlers run down the MRO).
        if self.sidebar_toc is not None:
            self.sidebar_toc.table_of_contents = message.table_of_contents
        message.prevent_default()
        message.stop()
