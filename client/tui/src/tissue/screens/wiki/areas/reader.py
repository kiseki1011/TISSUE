from __future__ import annotations

import logging

from textual import on
from textual.containers import Vertical
from textual.css.query import NoMatches
from textual.widgets import (
    Markdown,
    TextArea,
)

from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.links import _parse_link, _web_url
from tissue.screens.wiki.widgets import _WikiViewer

log = logging.getLogger(__name__)


class ReaderMixin(WikiScreenBase):
    """Routes link clicks (wiki/issue/project + web), the Contents outline, in-page
    anchors, and toggles the sidebar."""

    @on(Markdown.TableOfContentsSelected)
    def _on_toc_selected(self, message: Markdown.TableOfContentsSelected) -> None:
        message.stop()
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
            block = viewer.document.query_one(f"#{message.block_id}")
        except NoMatches:
            return
        viewer.scroll_to_widget(block, top=True)

    @on(Markdown.LinkClicked)
    def _on_link_clicked(self, message: Markdown.LinkClicked) -> None:
        message.stop()
        href = message.href
        if href.startswith("#"):  # in-document heading anchor
            self._goto_anchor(href[1:])
            return
        parsed = _parse_link(href)
        if parsed is None:
            # Not one of our internal schemes. A plain Markdown link to the web
            # (http/https/mailto) is handed to the OS browser; anything else we
            # can't route is ignored (with a hint so the click isn't silent).
            url = _web_url(href)
            if url is not None:
                self.app.open_url(url)
            else:
                log.debug("Wiki: ignoring unsupported link %r", href)
                self.app.notify(
                    "That link can't be opened (web links need an "
                    "http:// or https:// prefix).",
                    severity="warning",
                )
            return
        scheme, value = parsed
        if scheme == "wiki":
            try:
                doc_id = int(value)
            except ValueError:
                self.app.notify("That wiki link is malformed.", severity="warning")
                return
            self.run_worker(
                self._open_document(doc_id), exclusive=True, group="wiki-doc"
            )
        else:  # issue / project — no screen for these yet
            self.app.notify(
                f"Opening {scheme} links isn't available yet.", severity="warning"
            )

    def _goto_anchor(self, anchor: str) -> None:
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
        except NoMatches:
            return
        viewer.document.goto_anchor(anchor)

    def action_toggle_sidebar(self) -> None:
        # Toggle the whole left column (search bar + tabbed sidebar) so the
        # reading pane — info header + body — expands to the full width.
        try:
            col = self.query_one("#wiki-sidebar-col", Vertical)
        except NoMatches:
            return
        col.display = not col.display
        if col.display:
            # Land focus in the active tab's own content (consistent across all
            # tabs, and never flips the active tab — see _focus_sidebar_content).
            self.call_after_refresh(self._focus_sidebar_content)
        else:  # don't strand focus inside the now-hidden column
            if self._editing:
                # In authoring mode the editor (not the viewer) holds the content.
                try:
                    self.query_one("#wiki-editor", TextArea).focus()
                except NoMatches:
                    pass
                return
            try:
                viewer = self.query_one("#wiki-viewer", _WikiViewer)
            except NoMatches:
                return
            # Only when a document is open (viewer shown); before that the viewer
            # is hidden behind the placeholder, so focusing it would strand focus
            # on a non-displayed widget. Textual relocates focus off the hidden
            # column on its own in that case.
            if viewer.display:
                viewer.document.focus()
