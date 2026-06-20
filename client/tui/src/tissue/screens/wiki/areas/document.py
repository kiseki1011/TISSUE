from __future__ import annotations

import logging

from textual import on
from textual.containers import Horizontal
from textual.css.query import NoMatches
from textual.widgets import (
    OptionList,
    Rule,
    Static,
    Tree,
)

from tissue.api.errors import TissueApiError
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.widgets import _WikiViewer

log = logging.getLogger(__name__)


class DocumentMixin(WikiScreenBase):
    """Opens a document (from a tree node, search result, or parent link) into the
    reader, and closes it back to the placeholder."""

    @on(Tree.NodeSelected, "#wiki-tree")
    def _on_node_selected(self, event: Tree.NodeSelected[int]) -> None:
        doc_id = event.node.data
        if doc_id is not None:
            self.run_worker(
                self._open_document(doc_id), exclusive=True, group="wiki-doc"
            )

    @on(OptionList.OptionSelected, "#wiki-results")
    def _on_result_selected(self, event: OptionList.OptionSelected) -> None:
        if event.option.id is None:
            return
        self.run_worker(
            self._open_document(int(event.option.id)),
            exclusive=True,
            group="wiki-doc",
        )

    def action_open_parent(self, doc_id: int) -> None:
        # Fired by the parent link's `@click=screen.open_parent(<id>)` in the
        # meta header; opens the parent document.
        self.run_worker(self._open_document(doc_id), exclusive=True, group="wiki-doc")

    async def _open_document(self, doc_id: int) -> None:
        # Don't yank the user out of a draft they're writing (would lose edits).
        if self._editing:
            self.app.notify(
                "Save or cancel the current draft first.", severity="warning"
            )
            return
        client = self.app.client
        if client is None:
            return
        try:
            doc = await client.wiki.get_document(doc_id)
        except TissueApiError as e:
            log.debug("Wiki: failed to load document %s: %s", doc_id, e)
            self.app.notify("Couldn't load the document.", severity="error")
            return
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
        except NoMatches:
            return
        # First open: swap the centred placeholder out for the reading pane.
        try:
            self.query_one("#wiki-placeholder", Static).display = False
        except NoMatches:
            pass
        viewer.display = True
        body = (doc.content or "").strip() or "_This document is empty._"
        await viewer.document.update(body)
        viewer.scroll_home(animate=False)
        # Set the identity BEFORE rendering meta: _render_meta -> the bookmark
        # button reads _current_doc_id to decide its state, and _load_versions
        # guards on it.
        self._current_doc = doc
        self._current_doc_id = doc_id
        # Clear the previous doc's history so _render_meta's _populate_version_select
        # shows only "Current" until this doc's versions load.
        self._versions = None
        await self._render_meta(doc)
        # Load the version history in the background so opening stays snappy; it
        # populates the version <Select> once it arrives.
        self.run_worker(
            self._load_versions(doc_id), exclusive=True, group="wiki-versions"
        )

    def _close_document(self) -> None:
        """Reverse _open_document: hide the reading pane + info header and show
        the centred placeholder again."""
        self._current_doc_id = None
        self._current_doc = None
        self._versions = None
        for selector, kind in (
            ("#wiki-viewer", _WikiViewer),
            ("#wiki-meta", Horizontal),
            ("#wiki-meta-rule", Rule),
        ):
            try:
                self.query_one(selector, kind).display = False
            except NoMatches:
                pass
        try:
            self.query_one("#wiki-placeholder", Static).display = True
        except NoMatches:
            pass
