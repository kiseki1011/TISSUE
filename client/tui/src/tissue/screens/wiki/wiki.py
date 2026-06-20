from __future__ import annotations

import logging

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.css.query import NoMatches
from textual.widgets import (
    Button,
    Input,
    Label,
    OptionList,
    Rule,
    Select,
    Static,
    TabbedContent,
    TabPane,
    TextArea,
)
from textual.widgets.markdown import MarkdownTableOfContents

from tissue.screens.wiki.areas.authoring import AuthoringMixin
from tissue.screens.wiki.areas.document import DocumentMixin
from tissue.screens.wiki.areas.drafts import DraftsMixin
from tissue.screens.wiki.areas.focus import FocusMixin
from tissue.screens.wiki.areas.meta import MetaMixin
from tissue.screens.wiki.areas.reader import ReaderMixin
from tissue.screens.wiki.areas.search import SearchMixin
from tissue.screens.wiki.areas.tag_filter import TagFilterMixin
from tissue.screens.wiki.areas.tree import TreeMixin
from tissue.screens.wiki.constants import (
    _CURRENT_VERSION,
    _DEFAULT_VERSION_BUMP,
    _MIN_QUERY_LEN,
    _PLACEHOLDER_TEXT,
    _ROOT_PARENT,
    _VERSION_BUMP_OPTIONS,
)
from tissue.screens.wiki.widgets import _WikiTree, _WikiViewer

log = logging.getLogger(__name__)


class WikiScreen(
    TreeMixin,
    FocusMixin,
    SearchMixin,
    TagFilterMixin,
    DocumentMixin,
    MetaMixin,
    DraftsMixin,
    AuthoringMixin,
    ReaderMixin,
):
    """Wiki reader: a search bar over a tabbed sidebar (Contents / Documents /
    Bookmarks) beside a full-height Markdown viewer.

    Opening a document renders an info header above the body; searching swaps the
    tree for a flat result list. Assembled from the area mixins in `areas/`.
    """

    CSS_PATH = "wiki.tcss"

    BINDINGS = [
        # ctrl+\ (not ctrl+b — that's tmux's default prefix, which would swallow
        # the key inside tmux). ctrl+\ sends 0x1C, a real control char, so it
        # reaches the app on every terminal and passes through tmux untouched.
        Binding("ctrl+backslash", "toggle_sidebar", "sidebar", key_display="ctrl+\\"),
        # ctrl+/ — terminals send it as ctrl+underscore (0x1F); the kitty keyboard
        # protocol sends it as ctrl+slash. Bind both, display as ctrl+/.
        Binding(
            "ctrl+underscore,ctrl+slash",
            "focus_search",
            "search",
            key_display="ctrl+/",
        ),
        # ctrl+t toggles the draft preview while authoring (only active then —
        # see check_action). ctrl+t sends 0x14, a real control char the editor's
        # TextArea doesn't bind, so it reaches the screen and passes through tmux.
        Binding("ctrl+t", "toggle_preview", "preview"),
    ]

    def top_bar_breadcrumb(self) -> str:
        return "Wiki"

    def compose_content(self) -> ComposeResult:
        with Vertical(id="screen-body"):
            with Horizontal(id="wiki-body"):
                # Left column: the search bar sits atop the tabbed sidebar at the
                # same width, so the viewer on the right gets the full height.
                with Vertical(id="wiki-sidebar-col"):
                    search = Input(
                        placeholder=f"{_MIN_QUERY_LEN}+ chars",
                        id="wiki-search",
                    )
                    search.border_title = "Search"
                    yield search
                    with TabbedContent(id="wiki-sidebar", initial="wiki-tab-documents"):
                        with TabPane("Contents", id="wiki-tab-contents"):
                            # The outline needs the viewer's Markdown, which exists
                            # only after mount; it's mounted here in _build_toc().
                            yield Container(id="wiki-toc-holder")
                        with TabPane("Documents", id="wiki-tab-documents"):
                            # Tag filter sits ABOVE the tree (between the "+ New
                            # Doc" band and the tree). The "+" opens the catalog
                            # picker; chosen tags show as removable chips and narrow
                            # the document list (their union, combined with any
                            # keyword) into the results.
                            with Vertical(id="wiki-filter"):
                                with Horizontal(id="wiki-filter-header"):
                                    yield Label("Filter by tag", id="wiki-filter-label")
                                    yield Button("+", id="wiki-filter-add-btn")
                                yield Vertical(id="wiki-filter-chips")
                            tree = _WikiTree("Documents", id="wiki-tree")
                            tree.show_root = False
                            # Selecting a doc shouldn't also collapse it; expand
                            # with the arrow glyph or the space key instead.
                            tree.auto_expand = False
                            yield tree
                            # Search swaps the tree for this results list (rich
                            # items with a content snippet); hidden until then.
                            yield OptionList(id="wiki-results")
                            # Offline drafts saved locally; the section (header +
                            # list) is shown only when at least one draft exists.
                            yield Label("Local drafts", id="wiki-drafts-header")
                            yield OptionList(id="wiki-drafts")
                        with TabPane("Bookmarks", id="wiki-tab-bookmarks"):
                            yield OptionList(id="wiki-bookmarks")
                    # "+ New Doc" is overlaid (own layer, docked) onto the empty
                    # band just below the tab row — so it never pushes the tree
                    # (or any tab's content) down. It starts a blank draft.
                    yield Button("+ New Doc", id="wiki-newdoc-btn")
                with Vertical(id="wiki-content"):
                    # Info header (left: detail rows; right: version picker +
                    # action buttons) over a rule, then the body. Hidden until a
                    # doc opens — a centred placeholder shows until then.
                    with Horizontal(id="wiki-meta"):
                        yield Vertical(id="wiki-meta-info")
                        with Vertical(id="wiki-meta-controls"):
                            # Version picker + a 🔒/⭐ status marker. allow_blank=
                            # False with a seeded "Current" option, so there's no
                            # blank/prompt entry — only real versions show.
                            with Horizontal(id="wiki-version-row"):
                                yield Select(
                                    [("Current", _CURRENT_VERSION)],
                                    value=_CURRENT_VERSION,
                                    allow_blank=False,
                                    id="wiki-version-select",
                                )
                                yield Label("", id="wiki-meta-status")
                            # Shown only while browsing a non-current snapshot
                            # (toggled in _set_meta_buttons_visible): a warning that
                            # this revision is read-only. Hidden on Current.
                            yield Label(
                                "This is a snapshot of a previous version. "
                                "Cannot be modified.",
                                id="wiki-snapshot-warning",
                            )
                            # Action grid: [set parent][bookmark][lock] on top,
                            # [edit][delete] below (aligned under bookmark/lock via
                            # a leading spacer). set parent / edit / delete are not
                            # wired up yet.
                            with Vertical(id="wiki-meta-buttons"):
                                with Horizontal(classes="wiki-meta-btn-row"):
                                    yield Button("Set parent", id="wiki-set-parent-btn")
                                    yield Button("Bookmark", id="wiki-bookmark-btn")
                                    yield Button("Lock", id="wiki-lock-btn")
                                with Horizontal(classes="wiki-meta-btn-row"):
                                    yield Label("", classes="wiki-meta-btn-spacer")
                                    yield Button("Edit", id="wiki-edit-btn")
                                    yield Button("Delete", id="wiki-delete-btn")
                    # Authoring form: replaces the read-only meta header while a
                    # draft is being written. Mirrors the read meta's two columns —
                    # left = title + tags inputs, right = a parent picker (where the
                    # version picker sits when reading) over the save / cancel
                    # buttons. Hidden until +New Doc / a draft is opened.
                    with Horizontal(id="wiki-edit-meta"):
                        with Vertical(id="wiki-edit-info"):
                            with Horizontal(classes="wiki-edit-row"):
                                yield Label("Title:", classes="wiki-edit-key")
                                yield Input(
                                    placeholder="New document title",
                                    id="wiki-edit-title",
                                    classes="wiki-edit-input",
                                )
                            with Horizontal(classes="wiki-edit-row wiki-edit-tags-row"):
                                yield Label("Tags:", classes="wiki-edit-key")
                                yield Label("", id="wiki-edit-tags-display")
                        with Vertical(id="wiki-edit-controls"):
                            # Same slot/size as the read-mode version picker: choose
                            # whether the new doc is a root or a child of the doc
                            # that was open when +New Doc was pressed. Seeded with
                            # the root option (allow_blank=False so it's never
                            # empty); refilled in _enter_authoring.
                            yield Select(
                                [("New root document", _ROOT_PARENT)],
                                value=_ROOT_PARENT,
                                allow_blank=False,
                                id="wiki-parent-select",
                            )
                            # Edit mode reuses this slot for the version-bump
                            # picker instead (which semantic bump a content edit
                            # records). Shown only while editing an existing doc;
                            # the parent picker is hidden then (see
                            # _set_authoring_controls).
                            yield Select(
                                _VERSION_BUMP_OPTIONS,
                                value=_DEFAULT_VERSION_BUMP,
                                allow_blank=False,
                                id="wiki-version-bump-select",
                            )
                            # Button block: row 1 = preview/edit toggle | save
                            # draft, row 2 = (spacer) | save | cancel. The leading
                            # spacer keeps save directly under save draft; preview
                            # then juts out to the left (and cancel to the right) —
                            # intentional.
                            with Vertical(id="wiki-edit-buttons"):
                                with Horizontal(classes="wiki-edit-btn-row"):
                                    yield Button("Preview", id="wiki-preview-btn")
                                    yield Button("Save draft", id="wiki-draft-save-btn")
                                with Horizontal(classes="wiki-edit-btn-row"):
                                    yield Label("", classes="wiki-edit-btn-spacer")
                                    yield Button("Save", id="wiki-save-btn")
                                    yield Button("Cancel", id="wiki-edit-cancel-btn")
                    yield Rule(id="wiki-meta-rule", line_style="solid")
                    yield _WikiViewer(
                        "",
                        show_table_of_contents=False,
                        open_links=False,
                        id="wiki-viewer",
                    )
                    # The draft body editor (shown in authoring mode in place of
                    # the viewer). Plain text: markdown syntax highlighting needs
                    # the optional `textual[syntax]` extra, which isn't a dep.
                    yield TextArea(
                        "",
                        id="wiki-editor",
                        soft_wrap=True,
                        tab_behavior="focus",
                        show_line_numbers=False,
                    )
                    # Rendered preview of the draft body (toggled with the
                    # preview/edit button or ctrl+t while authoring); occupies the
                    # editor's slot. A separate viewer from #wiki-viewer so opening
                    # a document's rendered body is never clobbered by a preview.
                    yield _WikiViewer(
                        "",
                        show_table_of_contents=False,
                        open_links=False,
                        id="wiki-preview",
                    )
                    yield Static(_PLACEHOLDER_TEXT, id="wiki-placeholder")

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_tree(), exclusive=True, group="wiki-tree")
        self.run_worker(self._load_bookmarks(), exclusive=True, group="wiki-bookmarks")
        self.call_after_refresh(self._after_mount)

    def _after_mount(self) -> None:
        self._build_toc()
        self._reload_drafts()
        self._render_filter_chips()
        self._focus_documents()

    def _build_toc(self) -> None:
        """Mount the outline widget (bound to the viewer's Markdown) once the
        viewer — and its inner Markdown — exist."""
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
            holder = self.query_one("#wiki-toc-holder", Container)
        except NoMatches:
            return
        if not holder.children:
            toc = MarkdownTableOfContents(viewer.document, id="wiki-toc")
            holder.mount(toc)
            viewer.sidebar_toc = toc

    async def refresh_data(self) -> None:
        # Refresh resets to the full hierarchy, leaving any search behind.
        self._search_gen += 1  # invalidate any in-flight search (see _run_search)
        self._cancel_search_timer()  # drop any pending live-search keystroke
        self._search_results = None
        self._search_keyword = ""
        # A refresh returns to the full hierarchy, so the tag filter is cleared too.
        self._filter_tags = []
        self._render_filter_chips()
        try:
            # prevent(Input.Changed) so this programmatic clear doesn't post a
            # Changed message that _on_search_changed would turn into a fresh
            # debounce timer — re-arming the very search the :466 cancel just
            # dropped (it would fire ~0.2s later and bump _search_gen again).
            search = self.query_one("#wiki-search", Input)
            with self.prevent(Input.Changed):
                search.value = ""
        except NoMatches:
            pass
        self._show_search_mode(False)
        if await self._load_tree():
            # Only after a successful reload (not a transient load error): if the
            # open document is gone from the tree (deleted), close it.
            self._close_open_document_if_gone()
        await self._load_bookmarks()
        self._reload_drafts()

    def can_refresh(self) -> bool:
        # Block `r` while authoring so a refresh can't wipe the in-progress draft.
        return not self._editing

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        # The preview toggle is only meaningful (and only shown) while authoring.
        if action == "toggle_preview":
            return self._editing or None
        return super().check_action(action, parameters)
