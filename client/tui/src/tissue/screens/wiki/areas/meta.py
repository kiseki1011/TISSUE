from __future__ import annotations

import logging
from typing import cast

from rich.text import Text
from textual import on
from textual.containers import Horizontal, Vertical
from textual.content import Content
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import (
    Button,
    Label,
    OptionList,
    Rule,
    Select,
)
from textual.widgets._select import SelectCurrent
from textual.widgets.option_list import Option

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.constants import (
    _CURRENT_VERSION,
    _PARENT_TITLE_LIMIT,
)
from tissue.screens.wiki.rendering import (
    _label,
)
from tissue.screens.wiki.widgets import _WikiViewer
from tissue.util.datetime_fmt import format_relative

log = logging.getLogger(__name__)


class MetaMixin(WikiScreenBase):
    """Renders the header above the body (title/tags/version/links/timestamps),
    drives the version-snapshot picker, and the bookmark/lock buttons + markers."""

    async def _render_meta(self, doc: WikiDocumentDetail) -> None:
        """Fill the left column of the info header (title, tags, version /
        linked-issues, created / modified, parent link, author) and reveal the
        header + rule. Lock/bookmark state shows beside the version picker (see
        _update_meta_status), not in these rows."""
        try:
            meta = self.query_one("#wiki-meta", Horizontal)
            info = self.query_one("#wiki-meta-info", Vertical)
            rule = self.query_one("#wiki-meta-rule", Rule)
        except NoMatches:
            return
        # `or "Untitled"` after strip() so a whitespace-only title still shows a
        # label rather than a blank value.
        title = (doc.title or "").strip() or "Untitled"
        issue_count = self._linked_issue_count(doc)
        tags = [(t.name or "", t.color) for t in (doc.tags or []) if t.name]
        rows = [
            self._title_row(title),
            Horizontal(
                Label("Tags:", classes="meta-key-wide"),
                # Pills + an inline, clickable "+" right after the last tag, all in
                # one wrapping label — so the "+" flows after the final pill (and
                # wraps with the tags) instead of being shoved to the column edge.
                # The "+" opens the tag manager; only "+" shows when there are no
                # tags. Hidden while browsing a snapshot (show_add follows the
                # action-visibility state).
                Label(
                    self._tags_content(tags, show_add=self._meta_actions_visible),
                    id="wiki-meta-tags",
                    classes="meta-pair-value",
                ),
                classes="detail-row wiki-tags-row",
            ),
            self._pair2(
                "Version",
                str(doc.current_version or "-"),
                "Linked Issues",
                str(issue_count),
            ),
            # meta-key-wide (not the default auto-width key) so the date value
            # lines up in the same column as every other row's value.
            self._pair(
                "Created",
                format_relative(doc.created_at),
                key_class="meta-key-wide",
            ),
            self._pair(
                "Modified",
                format_relative(doc.last_modified_at),
                key_class="meta-key-wide",
            ),
            # Parent: its own row below Modified. A clickable link (opens the
            # parent), or "-" for a root document.
            Horizontal(
                Label("Parent:", classes="meta-key-wide"),
                self._parent_widget(doc),
                classes="detail-row",
            ),
            # Author is a "-" placeholder: created_by is only a member id, and
            # there's no general member-name lookup for non-admins.
            self._pair("Author", "-", key_class="meta-key-wide"),
        ]
        await info.remove_children()
        await info.mount_all(rows)
        # Show "Current (vX)" for THIS doc immediately (history arrives later via
        # _load_versions) so the picker never lingers on the previous doc's value.
        self._populate_version_select()
        self._update_bookmark_button()
        self._update_lock_button()
        # A freshly opened doc starts on Current, so the action buttons are shown
        # (they're only hidden while browsing a non-current snapshot).
        self._set_meta_buttons_visible(True)
        meta.display = True
        rule.display = True

    @staticmethod
    def _linked_issue_count(doc: WikiDocumentDetail) -> int:
        """Number of the document's outgoing links that target an issue."""
        return sum(1 for link in (doc.links or []) if link.target_type == "ISSUE")

    @staticmethod
    def _title_row(value: str) -> Horizontal:
        # Text() so a title with markup chars renders literally, never parsed.
        return Horizontal(
            Label("Title:", classes="meta-key-wide"),
            Label(Text(value), classes="wiki-meta-title"),
            classes="detail-row",
        )

    @staticmethod
    def _pair(
        key: str,
        value: str,
        *,
        value_id: str | None = None,
        key_class: str = "meta-pair-key",
    ) -> Horizontal:
        """One `key: value` cell. Pass a fixed-width `key_class` (e.g.
        meta-key-wide) to align several rows' values at the same column."""
        return Horizontal(
            Label(f"{key}:", classes=key_class),
            Label(value, id=value_id, classes="meta-pair-value"),
            classes="meta-pair",
        )

    @staticmethod
    def _pair2(
        key1: str,
        value1: str,
        key2: str,
        value2: str | Widget,
        *,
        value2_id: str | None = None,
    ) -> Horizontal:
        """A row with two aligned key:value columns. `value2` may be a ready-made
        widget (e.g. the clickable parent link) instead of plain text."""
        if isinstance(value2, Widget):
            v2: Widget = value2
        else:
            v2 = Label(value2, id=value2_id, classes="meta-pair-value")
        return Horizontal(
            Label(f"{key1}:", classes="meta-key-wide"),
            Label(value1, classes="meta-val-slot"),
            Label(f"{key2}:", classes="meta-key-2"),
            v2,
            classes="detail-row",
        )

    @staticmethod
    def _parent_widget(doc: WikiDocumentDetail) -> Widget:
        """Parent value: a clickable link opening the parent doc (open_parent
        action), or "-" for a root. Carried as literal Content with an `@click`
        span — not an interpolated markup string — so a title with stray brackets
        renders verbatim."""
        pid = doc.parent_document_id
        ptitle = (doc.parent_document_title or "").strip()
        if pid is None or not ptitle:
            return Label("-", classes="meta-pair-value")
        shown = (
            ptitle
            if len(ptitle) <= _PARENT_TITLE_LIMIT
            else ptitle[: _PARENT_TITLE_LIMIT - 1] + "…"
        )
        link = Content(shown).stylize(f"@click=screen.open_parent({pid})")
        return Label(link, id="wiki-meta-parent", classes="wiki-meta-parent-link")

    async def _load_versions(self, doc_id: int) -> None:
        """Fetch the document's version history and fill the version <Select>."""
        client = self.app.client
        if client is None:
            return
        try:
            versions = await client.wiki.list_versions(doc_id)
        except TissueApiError as e:
            log.debug("Wiki: failed to load versions for %s: %s", doc_id, e)
            versions = []
        if doc_id != self._current_doc_id:  # user opened another doc meanwhile
            return
        self._versions = list(versions)
        self._populate_version_select()

    def _populate_version_select(self) -> None:
        """Options = "Current (vX)" then every snapshot, newest first (no de-dup
        against the live version — the latest snapshot is still a real revision
        worth opening). Selection resets to Current."""
        try:
            select = self.query_one("#wiki-version-select", Select)
        except NoMatches:
            return
        doc = self._current_doc
        current = (doc.current_version if doc else None) or None
        label = f"Current (v{current})" if current else "Current"
        options: list[tuple[str, int]] = [(label, _CURRENT_VERSION)]
        for snapshot in self._versions or []:
            if snapshot.id is None:
                continue
            options.append((f"v{snapshot.snapshot_version or '?'}", snapshot.id))
        self._set_version_select(select, options, _CURRENT_VERSION)

    def _set_meta_buttons_visible(self, visible: bool) -> None:
        """Show/hide every meta action (the button stack + the inline Tags "+").
        Hidden while browsing a non-current snapshot, which is a read-only
        archive."""
        self._meta_actions_visible = visible
        self._set_display("#wiki-meta-buttons", Vertical, visible)
        # The read-only snapshot warning is the inverse: shown only while a
        # non-current revision is on screen (i.e. when actions are hidden).
        self._set_display("#wiki-snapshot-warning", Label, not visible)
        # Re-render the Tags row so the inline "+" appears/disappears with the rest.
        doc = self._current_doc
        tags = (
            [(t.name or "", t.color) for t in (doc.tags or []) if t.name]
            if doc is not None
            else []
        )
        self._render_meta_tags(tags)

    @staticmethod
    def _set_version_select(
        select: Select, options: list[tuple[str, int]] | None, value: int
    ) -> None:
        """Mutate the version <Select> without it echoing a Select.Changed back
        to us. The boolean-flag approach can't work: set_options/value post the
        Changed message (queued) and it's dispatched on a *later* turn, after any
        sync flag is reset. `select.prevent(...)` instead bakes the suppression
        into the message at post time (inside this block), so our own writes —
        including the NULL value set_options briefly assigns — are never handled
        as a user choice."""
        with select.prevent(Select.Changed):
            if options is not None:
                select.set_options(options)
            select.value = value
        # The collapsed display (SelectCurrent) is only refreshed by the value
        # watcher, which doesn't fire when the value is unchanged (e.g. value
        # stays Current while set_options swaps "Current (vA)" → "Current (vB)").
        # Refresh it explicitly so the collapsed control shows the version too,
        # not just the opened dropdown's option.
        try:
            current = select.query_one(SelectCurrent)
        except NoMatches:
            return
        label = next((lbl for lbl, val in select._options if val == select.value), None)
        if label is not None:
            current.update(label)

    @on(Select.Changed, "#wiki-version-select")
    def _on_version_changed(self, event: Select.Changed) -> None:
        # Select.NULL (not Select.BLANK — that resolves to Widget.BLANK==False and
        # never matches) is the "no selection" sentinel; ignore it.
        if event.value is Select.NULL:
            return
        doc_id = self._current_doc_id
        if doc_id is None:
            return
        # A non-current version is a read-only snapshot — hide the action buttons
        # (nothing can be done to an archived revision). Restored on Current / open.
        self._set_meta_buttons_visible(cast(int, event.value) == _CURRENT_VERSION)
        self.run_worker(
            self._view_version(doc_id, cast(int, event.value)),
            exclusive=True,
            group="wiki-version-view",
        )

    async def _view_version(self, doc_id: int, value: int) -> None:
        """Show the chosen version's content in the viewer (read-only browse).
        `_CURRENT_VERSION` restores the live content we already have."""
        if value == _CURRENT_VERSION:
            await self._set_viewer_body(
                self._current_doc.content if self._current_doc else ""
            )
            return
        client = self.app.client
        if client is None:
            return
        try:
            snapshot = await client.wiki.get_version(doc_id, value)
        except TissueApiError as e:
            log.debug("Wiki: failed to load version %s: %s", value, e)
            if doc_id != self._current_doc_id:  # switched docs while fetching
                return
            self.app.notify("Couldn't load that version.", severity="error")
            # Re-sync picker + viewer back to Current so the header doesn't claim
            # a version that isn't shown, and so re-picking the failed version is
            # a real value change next time (it would otherwise be a silent no-op).
            self._reset_version_select_to_current()
            self._set_meta_buttons_visible(True)  # back on Current → actions return
            await self._set_viewer_body(
                self._current_doc.content if self._current_doc else ""
            )
            return
        if doc_id != self._current_doc_id:  # switched docs while fetching
            return
        await self._set_viewer_body(snapshot.snapshot_content)

    def _reset_version_select_to_current(self) -> None:
        try:
            select = self.query_one("#wiki-version-select", Select)
        except NoMatches:
            return
        self._set_version_select(select, None, _CURRENT_VERSION)

    async def _set_viewer_body(self, content: str | None) -> None:
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
        except NoMatches:
            return
        body = (content or "").strip() or "_This document is empty._"
        await viewer.document.update(body)
        viewer.scroll_home(animate=False)

    def _is_bookmarked(self, doc_id: int) -> bool:
        return any(b.document_id == doc_id for b in (self._bookmarks or []))

    def _update_bookmark_button(self) -> None:
        """Reflect the open document's bookmark state on the toggle button (the
        label is the ACTION) and on the version-side status marker (⭐)."""
        doc_id = self._current_doc_id
        bookmarked = doc_id is not None and self._is_bookmarked(doc_id)
        try:
            self.query_one("#wiki-bookmark-btn", Button).label = (
                "Unbookmark" if bookmarked else "Bookmark"
            )
        except NoMatches:
            pass
        self._update_meta_status()

    def _update_meta_status(self) -> None:
        """Refresh the lock/bookmark marker beside the version picker
        (🔒 locked, ⭐ bookmarked)."""
        doc = self._current_doc
        doc_id = self._current_doc_id
        locked = bool(doc.locked) if doc else False
        bookmarked = doc_id is not None and self._is_bookmarked(doc_id)
        marks = []
        if locked:
            marks.append("🔒")
        if bookmarked:
            marks.append("⭐")
        try:
            self.query_one("#wiki-meta-status", Label).update(" ".join(marks))
        except NoMatches:
            pass

    @on(Button.Pressed, "#wiki-bookmark-btn")
    def _on_bookmark_pressed(self, event: Button.Pressed) -> None:
        doc_id = self._current_doc_id
        if doc_id is None:
            return
        self.run_worker(
            self._toggle_bookmark(doc_id),
            exclusive=True,
            group="wiki-bookmark-toggle",
        )

    def _update_lock_button(self) -> None:
        """Reflect the open document's lock state on the lock button (label is the
        ACTION) and on the version-side status marker (🔒). A locked document can't
        be edited (the server rejects it), so the Edit button is disabled while
        locked — it can't even be pressed."""
        locked = bool(self._current_doc.locked) if self._current_doc else False
        try:
            self.query_one("#wiki-lock-btn", Button).label = (
                "Unlock" if locked else "Lock"
            )
        except NoMatches:
            pass
        self._update_meta_status()
        try:
            edit_btn = self.query_one("#wiki-edit-btn", Button)
            edit_btn.disabled = locked
            edit_btn.tooltip = "Unlock to edit" if locked else None
        except NoMatches:
            pass

    @on(Button.Pressed, "#wiki-lock-btn")
    def _on_lock_pressed(self, event: Button.Pressed) -> None:
        doc_id = self._current_doc_id
        if doc_id is None:
            return
        self.run_worker(
            self._toggle_lock(doc_id), exclusive=True, group="wiki-lock-toggle"
        )

    async def _toggle_lock(self, doc_id: int) -> None:
        client = self.app.client
        if client is None or self._current_doc is None:
            return
        locked = bool(self._current_doc.locked)
        try:
            button = self.query_one("#wiki-lock-btn", Button)
        except NoMatches:
            button = None
        if button is not None:
            button.disabled = True  # block double-fires while the call is in flight
        try:
            if locked:
                await client.wiki.unlock(doc_id)
            else:
                await client.wiki.lock(doc_id)
        except TissueApiError as e:
            log.debug("Wiki: failed to toggle lock for %s: %s", doc_id, e)
            self.app.notify("Couldn't update the lock.", severity="error")
            return
        finally:
            if button is not None:
                button.disabled = False
        if doc_id != self._current_doc_id:  # switched docs while toggling
            return
        self._current_doc.locked = not locked
        self._update_lock_button()

    async def _toggle_bookmark(self, doc_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            button = self.query_one("#wiki-bookmark-btn", Button)
        except NoMatches:
            button = None
        if button is not None:
            button.disabled = True  # block double-fires while the call is in flight
        try:
            if self._is_bookmarked(doc_id):
                await client.wiki.remove_bookmark(doc_id)
            else:
                await client.wiki.add_bookmark(doc_id)
        except TissueApiError as e:
            log.debug("Wiki: failed to toggle bookmark for %s: %s", doc_id, e)
            self.app.notify("Couldn't update the bookmark.", severity="error")
            return
        finally:
            if button is not None:
                button.disabled = False
        # Reload so the Bookmarks tab and the button both reflect the new state.
        await self._load_bookmarks()

    async def _load_bookmarks(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            bookmarks = await client.wiki.list_bookmarks()
        except TissueApiError as e:
            log.debug("Wiki: failed to load bookmarks: %s", e)
            self._bookmarks = []
            self._populate_bookmarks()
            return
        self._bookmarks = list(bookmarks)
        self._populate_bookmarks()

    def _populate_bookmarks(self) -> None:
        try:
            options = self.query_one("#wiki-bookmarks", OptionList)
        except NoMatches:
            return
        options.clear_options()
        for bookmark in self._bookmarks or []:
            if bookmark.document_id is None:
                continue
            options.add_option(
                Option(
                    Text.assemble("📄 ", _label(bookmark.title)),
                    id=str(bookmark.document_id),
                )
            )
        # Keep the meta-area toggle in sync whenever the bookmark set changes.
        self._update_bookmark_button()

    @on(OptionList.OptionSelected, "#wiki-bookmarks")
    def _on_bookmark_selected(self, event: OptionList.OptionSelected) -> None:
        if event.option.id is None:
            return
        self.run_worker(
            self._open_document(int(event.option.id)),
            exclusive=True,
            group="wiki-doc",
        )
