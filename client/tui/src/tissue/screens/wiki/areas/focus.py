from __future__ import annotations

import logging

from textual.css.query import NoMatches
from textual.widgets import (
    OptionList,
    TabbedContent,
    Tree,
)
from textual.widgets.markdown import MarkdownTableOfContents

from tissue.screens.wiki._base import WikiScreenBase

log = logging.getLogger(__name__)


class FocusMixin(WikiScreenBase):
    """Moves focus to the active Documents-tab widget or the visible tab's content."""

    def _focus_documents(self) -> None:
        """Focus whichever Documents-tab widget is visible — the search-results
        list when searching, otherwise the hierarchy tree. No-op when the user
        is on another tab, so a late (post-search) focus can't yank them off it."""
        try:
            if self.query_one("#wiki-sidebar", TabbedContent).active != (
                "wiki-tab-documents"
            ):
                return
        except NoMatches:
            pass
        try:
            results = self.query_one("#wiki-results", OptionList)
            if results.display:
                results.focus()
                return
        except NoMatches:
            pass
        try:
            self.query_one("#wiki-tree", Tree).focus()
        except NoMatches:
            pass

    # Non-Documents tab id -> the focusable content widget inside that pane.
    _TAB_CONTENT_SELECTOR = {
        "wiki-tab-bookmarks": "#wiki-bookmarks",
        "wiki-tab-contents": "#wiki-toc",
    }

    def _focus_sidebar_content(self) -> None:
        """Focus the active tab's own content widget.

        Focusing the *active* pane's content is safe; focusing a different
        pane's widget would flip the tab (Textual auto-activates the pane of a
        newly-focused descendant), so we only ever target the active tab.
        """
        try:
            sidebar = self.query_one("#wiki-sidebar", TabbedContent)
        except NoMatches:
            return
        if sidebar.active == "wiki-tab-documents":
            self._focus_documents()
            return
        selector = self._TAB_CONTENT_SELECTOR.get(sidebar.active)
        if selector is None:
            return
        try:
            widget = self.query_one(selector)
        except NoMatches:
            return
        # The Contents outline (MarkdownTableOfContents) isn't focusable itself;
        # focus its inner Tree instead.
        if isinstance(widget, MarkdownTableOfContents):
            try:
                widget.query_one(Tree).focus()
            except NoMatches:
                pass
        else:
            widget.focus()
