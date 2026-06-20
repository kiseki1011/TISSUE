from __future__ import annotations

import logging

from textual import on
from textual.css.query import NoMatches
from textual.widgets import (
    Input,
    TabbedContent,
)

from tissue.api.errors import TissueApiError
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.constants import (
    _MIN_QUERY_LEN,
    _SEARCH_DEBOUNCE,
    _SEARCH_SIZE,
)

log = logging.getLogger(__name__)


class SearchMixin(WikiScreenBase):
    """Runs the search input (keyword + active tag filter) and shows the results."""

    def action_focus_search(self) -> None:
        # ctrl+/ — jump straight to the search input from anywhere on the screen.
        try:
            self.query_one("#wiki-search", Input).focus()
        except NoMatches:
            pass

    def _cancel_search_timer(self) -> None:
        if self._search_timer is not None:
            self._search_timer.stop()
            self._search_timer = None

    def on_unmount(self) -> None:
        # Don't let a pending debounce fire on a screen that's going away.
        self._cancel_search_timer()

    @on(Input.Changed, "#wiki-search")
    def _on_search_changed(self, event: Input.Changed) -> None:
        # Live search: (re)start the debounce timer so the search fires only once
        # typing pauses — not on every keystroke. Focus stays in the input (we
        # pass focus_results=False) so the user can keep typing.
        self._cancel_search_timer()
        value = event.value
        self._search_timer = self.set_timer(
            _SEARCH_DEBOUNCE,
            lambda: self.run_worker(
                self._run_search(value.strip()),
                exclusive=True,
                group="wiki-search",
            ),
        )

    @on(Input.Submitted, "#wiki-search")
    def _on_search_submitted(self, event: Input.Submitted) -> None:
        # Enter searches immediately (skips the debounce) and moves focus into
        # the shown Documents widget (the results list, or the tree for a query
        # below the minimum length) so it can be navigated.
        self._cancel_search_timer()
        self.run_worker(
            self._run_search(event.value.strip(), focus_results=True),
            exclusive=True,
            group="wiki-search",
        )

    def _rerun_search(self, *, focus_results: bool = False) -> None:
        """Re-run the search with the current keyword (used when the tag filter
        changes — the keyword stays put, only the filter moved)."""
        keyword = ""
        try:
            keyword = self.query_one("#wiki-search", Input).value.strip()
        except NoMatches:
            pass
        self.run_worker(
            self._run_search(keyword, focus_results=focus_results),
            exclusive=True,
            group="wiki-search",
        )

    async def _run_search(self, keyword: str, *, focus_results: bool = False) -> None:
        self._search_gen += 1
        gen = self._search_gen
        # The effective query is the keyword (only once it's long enough) plus the
        # active tag filter (union of its ids). Either alone is enough to search.
        kw = keyword if len(keyword) >= _MIN_QUERY_LEN else None
        tag_ids = [tid for tid, _, _ in self._filter_tags] or None
        if kw is None and tag_ids is None:  # nothing to search → browse the tree
            self._search_results = None
            self._search_keyword = ""
            self._show_search_mode(False)
            # Don't force the Documents tab here — the user may be browsing the
            # Contents/Bookmarks tab while clearing the query.
            if focus_results:
                self.call_after_refresh(self._focus_documents)
            return
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.wiki.search(
                keyword=kw, tag_ids=tag_ids, size=_SEARCH_SIZE
            )
        except TissueApiError as e:
            log.debug("Wiki: search failed: %s", e)
            self.app.notify("Search failed. Please try again.", severity="error")
            return
        if gen != self._search_gen:  # superseded by a newer search or a refresh
            return
        self._search_results = list(page.content or [])
        self._search_keyword = kw or ""  # tag-only filter has no keyword to bold
        self._populate_results()
        self._show_search_mode(True)
        self._activate_documents_tab()
        if not self._search_results:
            # The empty-state row (in _populate_results) is the feedback now.
            return
        if page.has_next:
            self.app.notify(
                f"Showing the first {len(self._search_results)} matches — "
                "refine to narrow."
            )
        if focus_results:
            self.call_after_refresh(self._focus_documents)

    def _activate_documents_tab(self) -> None:
        try:
            tabs = self.query_one("#wiki-sidebar", TabbedContent)
        except NoMatches:
            return
        tabs.active = "wiki-tab-documents"
