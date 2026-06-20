from __future__ import annotations

from typing import TYPE_CHECKING

from textual.css.query import NoMatches

from tissue.screens.base import RefreshableScreen

if TYPE_CHECKING:
    from textual.content import Content
    from textual.timer import Timer

    from tissue.api.generated.models.wiki_bookmark_response import WikiBookmarkResponse
    from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
    from tissue.api.generated.models.wiki_document_search_result import (
        WikiDocumentSearchResult,
    )
    from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
    from tissue.api.generated.models.wiki_snapshot_summary import WikiSnapshotSummary
    from tissue.app import TissueApp
    from tissue.domain.wiki.drafts import Draft, DraftStore
    from tissue.screens.wiki.modals import FilterTag


class WikiScreenBase(RefreshableScreen):
    """Shared base for the WikiScreen area mixins.

    Holds the screen's shared state (`__init__`) and, under `TYPE_CHECKING`, the
    cross-area method contract each mixin type-checks against. Real bodies live on
    whichever mixin owns the area; every mixin inherits this base.
    """

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self) -> None:
        super().__init__()
        self._tree_nodes: list[WikiDocumentTreeNode] | None = None
        # None = browsing the hierarchy tree; a list = showing search results.
        self._search_results: list[WikiDocumentSearchResult] | None = None
        # The keyword behind the current results, for snippet highlighting.
        self._search_keyword = ""
        # Bumped on every search and on refresh so a slow in-flight search whose
        # result lands late can't clobber a newer search or a refresh.
        self._search_gen = 0
        # Bookmarks tab: lists existing bookmarks and opens them.
        self._bookmarks: list[WikiBookmarkResponse] | None = None
        # The currently-open document, so a refresh can close it (back to the
        # placeholder) if it has been deleted on the server.
        self._current_doc_id: int | None = None
        # The live document detail, kept so the version <Select>'s "Current"
        # option can restore the live content without another fetch.
        self._current_doc: WikiDocumentDetail | None = None
        # Version history (snapshots) of the open document, newest first.
        self._versions: list[WikiSnapshotSummary] | None = None
        # Pending debounce timer for live search (restarted on each keystroke).
        self._search_timer: Timer | None = None
        # Authoring mode: True while the content pane is the draft editor (a new
        # document being written) rather than the read-only viewer.
        self._editing = False
        # The local draft file backing the current edit session, so "draft save"
        # overwrites it in place; None for a brand-new, never-saved draft.
        self._editing_draft: Draft | None = None
        # Working tag names for the draft being authored (chosen via the picker).
        self._draft_tags: list[str] = []
        # Authoring sub-mode: True while showing the rendered preview of the draft
        # (the #wiki-preview viewer) instead of the #wiki-editor.
        self._preview = False
        # Edit mode: the id of the existing document being edited (reusing the
        # authoring form), or None when authoring a brand-new document. Set on
        # Edit, cleared on save/cancel.
        self._edit_target_id: int | None = None
        # The opened document's title/content captured when editing began, so a
        # save only writes the fields that actually changed (and a content edit
        # is what bumps the version — a title-only edit must not).
        self._edit_original_title = ""
        self._edit_original_body = ""
        # Active tag filter: the (id, name, colour) tuples whose union narrows the
        # document list (empty = no filter). Combined with any keyword search.
        self._filter_tags: list[FilterTag] = []
        # Whether the meta action controls (buttons + the inline Tags "+") are
        # shown. Hidden while browsing a non-current snapshot (read-only archive).
        self._meta_actions_visible = True

    def _set_display(self, selector: str, kind: type, show: bool) -> None:
        """Show/hide a child widget, ignoring it if it isn't mounted."""
        try:
            self.query_one(selector, kind).display = show
        except NoMatches:
            pass

    if TYPE_CHECKING:
        # Cross-area methods: implemented by the mixin that owns the area, called
        # from others. Declared here so every mixin type-checks against them.
        async def _open_document(self, doc_id: int) -> None: ...
        def _close_document(self) -> None: ...
        def _close_open_document_if_gone(self) -> None: ...
        async def _load_tree(self) -> bool: ...
        def _populate_results(self) -> None: ...
        def _show_search_mode(self, on: bool) -> None: ...
        def _focus_documents(self) -> None: ...
        def _focus_sidebar_content(self) -> None: ...
        def _cancel_search_timer(self) -> None: ...
        def _rerun_search(self, *, focus_results: bool = False) -> None: ...
        def _render_filter_chips(self) -> None: ...
        def _tags_content(
            self, tags: list[tuple[str, str | None]], *, show_add: bool
        ) -> Content: ...
        async def _load_versions(self, doc_id: int) -> None: ...
        async def _render_meta(self, doc: WikiDocumentDetail) -> None: ...
        def _render_meta_tags(self, tags: list[tuple[str, str | None]]) -> None: ...
        def _populate_version_select(self) -> None: ...
        def _set_meta_buttons_visible(self, visible: bool) -> None: ...
        def _update_bookmark_button(self) -> None: ...
        def _update_lock_button(self) -> None: ...
        def _update_meta_status(self) -> None: ...
        async def _load_bookmarks(self) -> None: ...
        def _reload_drafts(self) -> None: ...
        def _draft_store(self) -> DraftStore: ...
        def _enter_authoring(self, draft: Draft | None) -> None: ...
        def _exit_authoring(self) -> None: ...
