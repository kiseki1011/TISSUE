from __future__ import annotations

import logging

from rich.text import Text
from textual.css.query import NoMatches
from textual.widgets import (
    OptionList,
    Tree,
)
from textual.widgets.option_list import Option
from textual.widgets.tree import TreeNode

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.rendering import (
    _build_result_text,
    _label,
)
from tissue.screens.wiki.widgets import _WikiTree

log = logging.getLogger(__name__)


class TreeMixin(WikiScreenBase):
    """Loads the document tree, builds its hierarchy, and swaps it for a flat
    result list while a search is active."""

    async def _load_tree(self) -> bool:
        """Load the document tree. Returns True if the fetch succeeded."""
        client = self.app.client
        if client is None:
            return False
        try:
            nodes = await client.wiki.get_tree()
        except TissueApiError as e:
            log.debug("Wiki: failed to load tree: %s", e)
            self.app.notify(
                "Couldn't load the wiki. Press r to retry.", severity="error"
            )
            self._tree_nodes = []
            self._populate_tree()
            return False
        self._tree_nodes = list(nodes)
        self._populate_tree()
        return True

    def _close_open_document_if_gone(self) -> None:
        """If a document is open but no longer present in the (just-reloaded)
        tree, close it back to the placeholder."""
        if self._current_doc_id is None:
            return
        ids = {n.id for n in (self._tree_nodes or []) if n.id is not None}
        if self._current_doc_id not in ids:
            self._close_document()
            self.app.notify("The open document is no longer available.")

    def _populate_tree(self) -> None:
        try:
            tree = self.query_one("#wiki-tree", _WikiTree)
        except NoMatches:
            return
        tree.clear()
        self._populate_hierarchy(tree)

    def _show_search_mode(self, on: bool) -> None:
        """Swap the Documents tab between the hierarchy tree (off) and the
        search-results list (on)."""
        try:
            tree = self.query_one("#wiki-tree", _WikiTree)
            results = self.query_one("#wiki-results", OptionList)
        except NoMatches:
            return
        tree.display = not on
        results.display = on
        if not on:
            results.clear_options()

    def _populate_results(self) -> None:
        try:
            results = self.query_one("#wiki-results", OptionList)
        except NoMatches:
            return
        results.clear_options()
        if not self._search_results:
            # Empty-state so a no-match search isn't just a blank panel.
            results.add_option(
                Option(Text("No matching documents", style="italic dim"), disabled=True)
            )
            return
        keyword = self._search_keyword
        primary = self.app.theme_variables.get("primary")
        for result in self._search_results or []:
            if result.id is None:
                continue
            text = _build_result_text(result, keyword=keyword, primary=primary)
            results.add_option(Option(text, id=str(result.id)))

    def _populate_hierarchy(self, tree: Tree[int]) -> None:
        nodes = self._tree_nodes or []
        by_id = {n.id: n for n in nodes if n.id is not None}
        # Group by parent. A node whose parent id is missing (no parent, or a
        # parent not in the set) is promoted to a root so nothing is dropped.
        children: dict[int | None, list[WikiDocumentTreeNode]] = {}
        for n in nodes:
            if n.id is None:
                continue
            pid = n.parent_document_id
            key = pid if (pid is not None and pid in by_id) else None
            children.setdefault(key, []).append(n)

        def add_children(
            parent: TreeNode[int], key: int | None, seen: set[int]
        ) -> None:
            for child in children.get(key, []):
                if child.id is None or child.id in seen:  # guard cycles / dupes
                    continue
                seen.add(child.id)
                node = parent.add(_label(child.title), data=child.id)
                if child.id in children:
                    add_children(node, child.id, seen)
                else:
                    node.allow_expand = False

        seen: set[int] = set()
        add_children(tree.root, None, seen)
        # Any node left unreached belongs to a parent-reference cycle (A↔B or a
        # self-parent); promote it to a root so no document is silently hidden.
        for n in nodes:
            if n.id is not None and n.id not in seen:
                seen.add(n.id)
                node = tree.root.add(_label(n.title), data=n.id)
                add_children(node, n.id, seen)
        tree.root.expand()
