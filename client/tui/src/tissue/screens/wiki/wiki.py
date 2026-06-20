from __future__ import annotations

import logging
from pathlib import Path
from typing import cast

from rich.color import Color, ColorParseError
from rich.style import Style
from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.content import Content
from textual.css.query import NoMatches
from textual.timer import Timer
from textual.widget import Widget
from textual.widgets import (
    Button,
    Input,
    Label,
    Markdown,
    MarkdownViewer,
    OptionList,
    Rule,
    Select,
    Static,
    TabbedContent,
    TabPane,
    TextArea,
    Tree,
)
from textual.widgets._select import SelectCurrent
from textual.widgets.markdown import MarkdownTableOfContents
from textual.widgets.option_list import Option
from textual.widgets.tree import TreeNode

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.wiki_bookmark_response import WikiBookmarkResponse
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
from tissue.api.generated.models.wiki_snapshot_summary import WikiSnapshotSummary
from tissue.paths import drafts_dir
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.wiki.drafts import Draft, DraftStore

log = logging.getLogger(__name__)

# Shown (italic, centred) in the reading pane before any document is opened.
_PLACEHOLDER_TEXT = "Select a document"

# Tree titles longer than this are clipped with a trailing "…".
_TITLE_LIMIT = 24

# Max search results fetched at once.
_SEARCH_SIZE = 50

# Live search: minimum query length to search, and the debounce (seconds) we
# wait after the last keystroke before firing so we don't hit the API per key.
_MIN_QUERY_LEN = 2
_SEARCH_DEBOUNCE = 0.2

# Sentinel value for the version <Select>'s "Current" option (the live document).
# Snapshot ids are positive, so 0 can never collide with a real snapshot.
_CURRENT_VERSION = 0

# Sentinel for the authoring parent <Select>'s "new root document" option (no
# parent). Real document ids are positive, so 0 never collides with one.
_ROOT_PARENT = 0

# The parent-document link in the meta header is clipped to this many chars.
_PARENT_TITLE_LIMIT = 20

# Inline wiki-link schemes: [text](wiki:ID) / [text](issue:KEY) / [text](project:KEY).
_LINK_SCHEMES = ("wiki", "issue", "project")

# A wiki document may carry at most 5 tags (server-enforced). The tag picker
# enforces this and the per-name length; we re-cap on publish defensively.
_MAX_TAGS = 5
# Server limits on a new document (CreateDocumentRequest): title 1-200 chars,
# content up to 100000. Checked client-side so over-limit input gets a clear
# message instead of a generic "couldn't save" from the server.
_TITLE_MAX = 200
_CONTENT_MAX = 100_000


def _parse_link(href: str) -> tuple[str, str] | None:
    """Split a `scheme:value` wiki link into (scheme, value); None if not one."""
    for scheme in _LINK_SCHEMES:
        prefix = f"{scheme}:"
        if href.startswith(prefix):
            return scheme, href[len(prefix) :]
    return None


def _tag_style(color: str | None) -> str:
    """A Rich style for a tag's color enum (e.g. "BRIGHT_BLUE" → "bright_blue"),
    or "" when the name isn't a color Rich understands."""
    if not color:
        return ""
    name = color.strip().lower()
    try:
        Color.parse(name)
    except ColorParseError:
        return ""
    return name


def _tags_text(tags: list[tuple[str, str | None]]) -> Text:
    """Render `(name, color)` tags as one line — each name in its color, two
    spaces apart. Empty → a dim dash."""
    if not tags:
        return Text("-", style="dim")
    text = Text()
    for i, (name, color) in enumerate(tags):
        if i:
            text.append("  ")
        text.append(name, style=_tag_style(color))
    return text


class _WikiViewer(MarkdownViewer):
    """MarkdownViewer adapted for the wiki reader.

    Two behaviours of the stock viewer get in our way:

    - it routes every link click to ``go()`` → ``document.load(path)``, which
      raises ``FileNotFoundError`` for our ``wiki:ID`` (or any non-file) links and
      trips Textual's error screen; and
    - it consumes the ``TableOfContentsUpdated`` message to feed its own (hidden)
      table of contents, so the outline never reaches our sidebar.

    So we swallow link clicks (real routing is a later chunk) and forward the
    outline straight to the sidebar outline widget (``sidebar_toc``) the screen
    registers once it's built.
    """

    # The sidebar outline to feed; set by the screen once it has been mounted.
    sidebar_toc: MarkdownTableOfContents | None = None

    async def _on_markdown_link_clicked(self, message: Markdown.LinkClicked) -> None:
        # Skip the stock MarkdownViewer.go() (it treats every href as a filesystem
        # path → FileNotFoundError). prevent_default() breaks the MRO walk so go()
        # never runs; we deliberately do NOT stop the message, so it bubbles up to
        # WikiScreen, which routes wiki: / issue: / project: links.
        message.prevent_default()

    def _on_markdown_table_of_contents_updated(
        self, message: Markdown.TableOfContentsUpdated
    ) -> None:
        # Forward the outline to the sidebar widget, then prevent_default() so the
        # stock handler (which would rebuild the viewer's own hidden TOC) is
        # skipped — stop() alone wouldn't suppress it (handlers run down the MRO).
        if self.sidebar_toc is not None:
            self.sidebar_toc.table_of_contents = message.table_of_contents
        message.prevent_default()
        message.stop()


_TOGGLE_META = Style.from_meta({"toggle": True})


class _WikiTree(Tree[int]):
    """Document tree that replaces the expand/collapse triangle with a book/page
    icon. A doc that has sub-docs shows 📖; a childless doc shows 📄.

    The book icon stays clickable to expand/collapse (the triangle's old job);
    clicking a label (or pressing Enter) opens the doc.
    """

    def render_label(
        self, node: TreeNode[int], base_style: Style, style: Style
    ) -> Text:
        label = node._label.copy()  # _label is the Text; .label getter is TextType
        label.stylize(style)
        if node.parent is None:  # the hidden root, never shown
            return label
        # A doc with sub-docs shows 📖 (clickable to expand/collapse); a leaf doc
        # shows 📄. No [+]/[-] indicator — the icon itself is the toggle.
        icon = "📖 " if node.children else "📄 "
        toggle = _TOGGLE_META if node.allow_expand else Style()
        return Text.assemble((icon, base_style + toggle), label)


class WikiScreen(RefreshableScreen):
    """Wiki reader: a tabbed sidebar (outline / document tree) + a Markdown viewer.

    Reader-first redesign built on Textual's `MarkdownViewer`. The left column
    stacks the search bar over a tabbed sidebar (Contents outline / Documents
    tree / read-only Bookmarks) at a shared width; the viewer fills the rest and
    runs full-height. The viewer's own table of contents is disabled — the
    outline feeds the "Contents" tab instead — and the whole sidebar toggles
    with ctrl+b. Searching swaps the Documents tree to a flat result list.
    Opening a document renders an info header (title, version, lock state,
    timestamps) above an `---` rule, then the body. Wiki/issue/project links are
    routed in `_on_link_clicked`.
    """

    CSS_PATH = "wiki.tcss"

    BINDINGS = [
        Binding("ctrl+b", "toggle_sidebar", "sidebar"),
        # ctrl+/ — terminals send it as ctrl+underscore (0x1F); the kitty keyboard
        # protocol sends it as ctrl+slash. Bind both, display as ctrl+/.
        Binding(
            "ctrl+underscore,ctrl+slash",
            "focus_search",
            "search",
            key_display="ctrl+/",
        ),
    ]

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
        # Bookmarks tab is read-only: it lists existing bookmarks and opens them.
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
                    # Fixed document-info header above a heavy rule, then the body
                    # in the viewer. The header is two columns: the stacked detail
                    # rows (Title / Version / Locked / Created / Last modified) on
                    # the left, and a top-right control stack — a version picker
                    # over a bookmark toggle. Header, rule and viewer stay hidden
                    # until a doc opens; until then a centred placeholder (aligned
                    # with the tree) is shown instead.
                    with Horizontal(id="wiki-meta"):
                        yield Vertical(id="wiki-meta-info")
                        with Vertical(id="wiki-meta-controls"):
                            # allow_blank=False (with a seeded "Current" option so
                            # the widget isn't empty at construction) means there's
                            # no blank/prompt entry — only real versions show.
                            yield Select(
                                [("Current", _CURRENT_VERSION)],
                                value=_CURRENT_VERSION,
                                allow_blank=False,
                                id="wiki-version-select",
                            )
                            # Action grid: [set parent][bookmark][lock] on top,
                            # [edit][delete] below (aligned under bookmark/lock via
                            # a leading spacer). set parent / edit / delete are not
                            # wired up yet.
                            with Vertical(id="wiki-meta-buttons"):
                                with Horizontal(classes="wiki-meta-btn-row"):
                                    yield Button("set parent", id="wiki-set-parent-btn")
                                    yield Button("bookmark", id="wiki-bookmark-btn")
                                    yield Button("lock", id="wiki-lock-btn")
                                with Horizontal(classes="wiki-meta-btn-row"):
                                    yield Label("", classes="wiki-meta-btn-spacer")
                                    yield Button("edit", id="wiki-edit-btn")
                                    yield Button("delete", id="wiki-delete-btn")
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
                            with Horizontal(classes="wiki-edit-row"):
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
                            # 2x2 button block: row 1 = save draft (a spacer holds
                            # the empty top-right cell so save draft lines up above
                            # save), row 2 = save | cancel.
                            with Vertical(id="wiki-edit-buttons"):
                                with Horizontal(classes="wiki-edit-btn-row"):
                                    yield Button("save draft", id="wiki-draft-save-btn")
                                    yield Label("", classes="wiki-edit-btn-spacer")
                                with Horizontal(classes="wiki-edit-btn-row"):
                                    yield Button("save", id="wiki-save-btn")
                                    yield Button("cancel", id="wiki-edit-cancel-btn")
                    yield Rule(id="wiki-meta-rule", line_style="heavy")
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
                    yield Static(_PLACEHOLDER_TEXT, id="wiki-placeholder")

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_tree(), exclusive=True, group="wiki-tree")
        self.run_worker(self._load_bookmarks(), exclusive=True, group="wiki-bookmarks")
        self.call_after_refresh(self._after_mount)

    def _after_mount(self) -> None:
        self._build_toc()
        self._reload_drafts()
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
        try:
            self.query_one("#wiki-search", Input).value = ""
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

    # ---- tree load -----------------------------------------------------

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

    # ---- search results (OptionList) -----------------------------------

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
        for result in self._search_results or []:
            if result.id is None:
                continue
            results.add_option(
                Option(self._build_result_text(result), id=str(result.id))
            )

    def _build_result_text(self, result: WikiDocumentSearchResult) -> Text:
        """One result: `📄 Title` (flush, keyword highlighted) plus a content
        snippet line when the keyword matched the body but not the title."""
        title = (result.title or "").strip() or "Untitled"
        keyword = self._search_keyword
        primary = self.app.theme_variables.get("primary")
        kw_style = f"bold on {primary}" if primary else "bold reverse"
        text = Text()
        text.append("📄 ")
        # Highlight the keyword in the title too (rest of the title is normal).
        self._append_highlighted(text, self._label(title).plain, keyword, kw_style, "")
        if keyword and keyword.casefold() not in title.casefold():
            snippet = self._build_snippet(
                result.content_snippet or "", keyword, kw_style
            )
            if snippet is not None and snippet.plain:
                text.append("\n")
                text.append_text(snippet)
        return text

    @staticmethod
    def _append_highlighted(
        text: Text, content: str, keyword: str, kw_style: str, base_style: str
    ) -> None:
        """Append `content`, styling every case-insensitive occurrence of
        `keyword` with `kw_style` and the surrounding text with `base_style`."""
        if not keyword:
            text.append(content, style=base_style)
            return
        low = content.casefold()
        kl = keyword.casefold()
        i = 0
        while True:
            j = low.find(kl, i)
            if j == -1:
                text.append(content[i:], style=base_style)
                return
            if j > i:
                text.append(content[i:j], style=base_style)
            text.append(content[j : j + len(keyword)], style=kw_style)
            i = j + len(keyword)

    def _build_snippet(self, snippet: str, keyword: str, kw_style: str) -> Text | None:
        """~30 chars before / 50 after the keyword (whitespace collapsed, no
        markdown). Keyword is bold on a primary background; the rest is dim."""
        flat = " ".join(snippet.split())
        if not flat:
            return None
        dim_style = "dim"
        text = Text()
        idx = flat.casefold().find(keyword.casefold())
        if idx == -1:
            # Keyword not literally present (e.g. a stemmed FTS match) — just dim
            # the start so there's still a preview.
            text.append(flat[:80], style=dim_style)
            if len(flat) > 80:
                text.append("…", style=dim_style)
            return text
        start = max(0, idx - 30)
        end = idx + len(keyword) + 50
        if start > 0:
            text.append("…", style=dim_style)
        self._append_highlighted(text, flat[start:end], keyword, kw_style, dim_style)
        if end < len(flat):
            text.append("…", style=dim_style)
        return text

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
                node = parent.add(self._label(child.title), data=child.id)
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
                node = tree.root.add(self._label(n.title), data=n.id)
                add_children(node, n.id, seen)
        tree.root.expand()

    @staticmethod
    def _label(title: str | None) -> Text:
        text = title or "Untitled"
        if len(text) > _TITLE_LIMIT:
            text = text[:_TITLE_LIMIT] + "…"
        return Text(text)

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

    # ---- search --------------------------------------------------------

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

    async def _run_search(self, keyword: str, *, focus_results: bool = False) -> None:
        self._search_gen += 1
        gen = self._search_gen
        if len(keyword) < _MIN_QUERY_LEN:  # too short → browse the hierarchy tree
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
            page = await client.wiki.search(keyword=keyword, size=_SEARCH_SIZE)
        except TissueApiError as e:
            log.debug("Wiki: search failed: %s", e)
            self.app.notify("Search failed. Please try again.", severity="error")
            return
        if gen != self._search_gen:  # superseded by a newer search or a refresh
            return
        self._search_results = list(page.content or [])
        self._search_keyword = keyword
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

    # ---- document render -----------------------------------------------

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

    async def _render_meta(self, doc: WikiDocumentDetail) -> None:
        """Fill the left column of the info header in two aligned columns:

            Title:     <title>
            Tags:      <tags>
            Version:   <v>        Locked:    <🔒/->
            Created:   <…>        Bookmark:  <⭐/->
            Modified:  <…>        Parent:    <parent link or ->
            Author:    <author or ->

        The parent value is a clickable link (opens the parent document); "-"
        when the doc is a root. The right-hand control stack (version picker +
        action buttons) is persistent — only its state is refreshed — then reveal
        header and rule."""
        try:
            meta = self.query_one("#wiki-meta", Horizontal)
            info = self.query_one("#wiki-meta-info", Vertical)
            rule = self.query_one("#wiki-meta-rule", Rule)
        except NoMatches:
            return
        # `or "Untitled"` after strip() so a whitespace-only title still shows a
        # label rather than a blank value.
        title = (doc.title or "").strip() or "Untitled"
        doc_id = self._current_doc_id
        bookmarked = doc_id is not None and self._is_bookmarked(doc_id)
        # Two aligned columns. Left column = Title / Version / Created / Last
        # Modified / Author (all keys fixed-width so values share one column).
        # Second column = the Locked/Bookmark status, beside Version and Created;
        # _pair2 puts the left value in a fixed-width slot so this second column
        # lines up across both rows.
        tags = [(t.name or "", t.color) for t in (doc.tags or []) if t.name]
        rows = [
            self._title_row(title),
            Horizontal(
                Label("Tags:", classes="meta-key-wide"),
                Label(_tags_text(tags), id="wiki-meta-tags", classes="meta-pair-value"),
                classes="detail-row",
            ),
            self._pair2(
                "Version",
                str(doc.current_version or "-"),
                "Locked",
                "🔒" if doc.locked else "-",
                value2_id="wiki-meta-locked",
            ),
            self._pair2(
                "Created",
                format_relative(doc.created_at),
                "Bookmark",
                "⭐" if bookmarked else "-",
                value2_id="wiki-meta-bookmarked",
            ),
            self._pair2(
                "Modified",
                format_relative(doc.last_modified_at),
                "Parent",
                self._parent_widget(doc),
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
        meta.display = True
        rule.display = True

    @staticmethod
    def _title_row(value: str) -> Horizontal:
        # Fixed-width key (meta-key-wide) so the title value aligns in the same
        # column as every other row's value. The value is bold + primary (mirrors
        # the dashboard Key styling) and wrapped in Text so the title always
        # renders literally, never parsed as markup.
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
        """One `key: value` cell. Cells are content-sized and pack to the left
        (see .meta-pair), so values sit right after their key whether the cell
        stands alone on a row or shares the row with sibling cells. Pass a
        fixed-width `key_class` (e.g. meta-key-wide) to line several rows' values
        up at the same column."""
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
        """A row with two aligned key:value columns. The first key is fixed-width
        (meta-key-wide) and its value sits in a fixed-width slot (meta-val-slot),
        so the second column — a fixed-width key + value — lines up at the same
        position across every row that uses `_pair2`. `value2` may be a ready-made
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
        """The Parent value: a clickable link that opens the parent document
        (via the screen's `open_parent` action), or a plain "-" for a root doc.
        The title is clipped, then carried as a literal Content with an `@click`
        style span — NOT interpolated into a markup string — so a title with
        stray brackets (e.g. "TODO [refactor") renders verbatim instead of being
        mis-parsed as markup."""
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

    # ---- version picker ------------------------------------------------

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
        """Options = "Current (vX)" then the older snapshots, newest first. The
        snapshot whose version equals the live version is dropped so it isn't
        listed twice. Selection resets to Current.

        Called twice per open: once from _render_meta with `_versions` still None
        (so it shows just "Current (vX)" for the new doc immediately, no stale
        history from the previous doc), then again from _load_versions once the
        history arrives."""
        try:
            select = self.query_one("#wiki-version-select", Select)
        except NoMatches:
            return
        doc = self._current_doc
        current = (doc.current_version if doc else None) or None
        label = f"Current (v{current})" if current else "Current"
        options: list[tuple[str, int]] = [(label, _CURRENT_VERSION)]
        for snapshot in self._versions or []:
            if snapshot.id is None or snapshot.snapshot_version == current:
                continue
            options.append((f"v{snapshot.snapshot_version or '?'}", snapshot.id))
        self._set_version_select(select, options, _CURRENT_VERSION)

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

    # ---- bookmarks -----------------------------------------------------

    def _is_bookmarked(self, doc_id: int) -> bool:
        return any(b.document_id == doc_id for b in (self._bookmarks or []))

    def _update_bookmark_button(self) -> None:
        """Reflect the open document's bookmark state on the toggle button (the
        label is the ACTION) and on the meta "Bookmark" marker (⭐/-)."""
        doc_id = self._current_doc_id
        bookmarked = doc_id is not None and self._is_bookmarked(doc_id)
        try:
            self.query_one("#wiki-bookmark-btn", Button).label = (
                "unbookmark" if bookmarked else "bookmark"
            )
        except NoMatches:
            pass
        try:
            self.query_one("#wiki-meta-bookmarked", Label).update(
                "⭐" if bookmarked else "-"
            )
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

    # ---- lock toggle ---------------------------------------------------

    def _update_lock_button(self) -> None:
        """Reflect the open document's lock state on the lock button (label is the
        ACTION) and on the meta "Locked" marker (🔒/-)."""
        locked = bool(self._current_doc.locked) if self._current_doc else False
        try:
            self.query_one("#wiki-lock-btn", Button).label = (
                "unlock" if locked else "lock"
            )
        except NoMatches:
            pass
        try:
            self.query_one("#wiki-meta-locked", Label).update("🔒" if locked else "-")
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
                    Text.assemble("📄 ", self._label(bookmark.title)),
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

    # ---- offline drafts ------------------------------------------------

    def _draft_store(self) -> DraftStore:
        """A store rooted at the configured draft folder (or the default)."""
        configured = self.app.config.settings.wiki_draft_dir
        root = Path(configured).expanduser() if configured else drafts_dir()
        return DraftStore(root)

    def _reload_drafts(self) -> None:
        """Repopulate the Local drafts list. The section (header + list) is shown
        only when at least one draft exists, so it doesn't waste space when empty.
        """
        try:
            header = self.query_one("#wiki-drafts-header", Label)
            options = self.query_one("#wiki-drafts", OptionList)
        except NoMatches:
            return
        drafts = self._draft_store().list_drafts()
        options.clear_options()
        for draft in drafts:
            if draft.path is None:
                continue
            options.add_option(Option(self._draft_label(draft), id=str(draft.path)))
        has_drafts = bool(drafts)
        header.display = has_drafts
        options.display = has_drafts

    def _draft_label(self, draft: Draft) -> Text:
        text = Text.assemble("📝 ", self._label(draft.title))
        modified = draft.modified_at()
        if modified is not None:
            text.append(f"  {format_relative(modified)}", style="dim")
        return text

    @on(OptionList.OptionSelected, "#wiki-drafts")
    def _on_draft_selected(self, event: OptionList.OptionSelected) -> None:
        if event.option.id is None:
            return
        if self._editing:
            self.app.notify(
                "Save or cancel the current draft first.", severity="warning"
            )
            return
        path = Path(event.option.id)
        try:
            draft = Draft.from_file(path)
        except OSError as e:
            log.warning("Wiki: couldn't open draft %s: %s", path, e)
            self.app.notify("Couldn't open that draft.", severity="error")
            self._reload_drafts()  # it may have been removed/renamed externally
            return
        self._enter_authoring(draft)

    # ---- authoring (new document) --------------------------------------

    @on(Button.Pressed, "#wiki-newdoc-btn")
    def _on_new_doc_pressed(self, event: Button.Pressed) -> None:
        if self._editing:
            self.app.notify("You're already writing a draft.", severity="warning")
            return
        self._enter_authoring(None)

    @on(Button.Pressed, "#wiki-edit-cancel-btn")
    def _on_edit_cancel_pressed(self, event: Button.Pressed) -> None:
        self._exit_authoring()

    @on(Button.Pressed, "#wiki-draft-save-btn")
    def _on_draft_save_pressed(self, event: Button.Pressed) -> None:
        draft = self._collect_draft()
        if draft is None:
            return
        try:
            self._draft_store().save(draft)
        except OSError as e:
            log.warning("Wiki: draft save failed: %s", e)
            self.app.notify("Couldn't save the draft.", severity="error")
            return
        # Now backed by a file, so subsequent draft-saves overwrite it in place.
        self._editing_draft = draft
        name = draft.path.name if draft.path else "draft"
        self.app.notify(f"Draft saved ({name}).")
        self._reload_drafts()

    @on(Button.Pressed, "#wiki-save-btn")
    def _on_save_pressed(self, event: Button.Pressed) -> None:
        draft = self._collect_draft()
        if draft is None:
            return
        self.run_worker(self._publish(draft), exclusive=True, group="wiki-publish")

    # ---- tags ----------------------------------------------------------
    # Tags are shown (read meta + draft editor) but the add/remove UI was
    # removed pending a redesign of how tags are chosen. `_draft_tags` is still
    # populated when a saved draft (with frontmatter tags) is opened, and is
    # attached on publish; the TagPickerModal + WikiService tag methods are kept
    # for the future entry point.

    def _update_draft_tags_display(self) -> None:
        try:
            label = self.query_one("#wiki-edit-tags-display", Label)
        except NoMatches:
            return
        label.update(_tags_text([(name, None) for name in self._draft_tags]))

    def _enter_authoring(self, draft: Draft | None) -> None:
        """Switch the content pane to the draft editor. `draft` pre-fills the
        form (continuing a saved draft); None starts blank."""
        # Resolve the editor widgets BEFORE mutating any state, so a missing
        # widget can't strand us in a half-entered editing mode (_editing=True
        # with the reader hidden but no editor shown — and `r` then suppressed).
        try:
            title = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return
        self._editing = True
        self._editing_draft = draft
        self._draft_tags = list(draft.tags) if draft else []
        self.refresh_bindings()  # `r` is suppressed while editing (can_refresh)
        for selector, kind in (
            ("#wiki-meta", Horizontal),
            ("#wiki-viewer", _WikiViewer),
            ("#wiki-placeholder", Static),
        ):
            self._set_display(selector, kind, False)
        title.value = draft.title if draft else ""
        editor.text = draft.body if draft else ""
        self._populate_parent_select()
        self._update_draft_tags_display()
        self._set_display("#wiki-edit-meta", Horizontal, True)
        self._set_display("#wiki-meta-rule", Rule, True)
        self._set_display("#wiki-editor", TextArea, True)
        title.focus()

    def _exit_authoring(self) -> None:
        """Leave the editor, restoring the previously-open document (or the
        placeholder when none was open)."""
        self._editing = False
        self._editing_draft = None
        self._draft_tags = []
        self.refresh_bindings()
        self._set_display("#wiki-edit-meta", Horizontal, False)
        self._set_display("#wiki-editor", TextArea, False)
        if self._current_doc_id is not None and self._current_doc is not None:
            self._set_display("#wiki-meta", Horizontal, True)
            self._set_display("#wiki-meta-rule", Rule, True)
            self._set_display("#wiki-viewer", _WikiViewer, True)
        else:
            self._set_display("#wiki-meta-rule", Rule, False)
            self._set_display("#wiki-placeholder", Static, True)

    def _set_display(self, selector: str, kind: type, show: bool) -> None:
        try:
            self.query_one(selector, kind).display = show
        except NoMatches:
            pass

    def _populate_parent_select(self) -> None:
        """Fill the authoring parent picker: always a "New root document" option,
        plus "Child of: <title>" when a document was open as authoring began (the
        viewed doc becomes the new doc's parent). Reset to root each time."""
        try:
            select = self.query_one("#wiki-parent-select", Select)
        except NoMatches:
            return
        options: list[tuple[str, int]] = [("New root document", _ROOT_PARENT)]
        if self._current_doc_id is not None and self._current_doc is not None:
            title = (self._current_doc.title or "Untitled").strip() or "Untitled"
            shown = (
                title
                if len(title) <= _PARENT_TITLE_LIMIT
                else title[: _PARENT_TITLE_LIMIT - 1] + "…"
            )
            options.append((f"Child of: {shown}", self._current_doc_id))
        select.set_options(options)
        select.value = _ROOT_PARENT

    def _selected_parent_id(self) -> int | None:
        """The parent chosen in the authoring picker: a real document id, or None
        for a root document (the _ROOT_PARENT sentinel / no selection)."""
        try:
            select = self.query_one("#wiki-parent-select", Select)
        except NoMatches:
            return None
        value = select.value
        # Select.NULL (not Select.BLANK — that resolves to Widget.BLANK==False and
        # never matches) is the "no selection" sentinel; _ROOT_PARENT means root.
        if value is Select.NULL or value == _ROOT_PARENT:
            return None
        return cast(int, value)

    def _collect_draft(self) -> Draft | None:
        """Build a Draft from the form. Returns None (and surfaces why) when the
        title is missing. Reuses the open draft's path so a save overwrites it."""
        try:
            title_input = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return None
        title = title_input.value.strip()
        if not title:
            self.app.notify("A title is required.", severity="warning")
            title_input.focus()
            return None
        if len(title) > _TITLE_MAX:
            self.app.notify(
                f"Title is too long (max {_TITLE_MAX} characters).",
                severity="warning",
            )
            title_input.focus()
            return None
        body = editor.text
        if len(body) > _CONTENT_MAX:
            self.app.notify(
                f"Content is too long (max {_CONTENT_MAX} characters).",
                severity="warning",
            )
            return None
        path = self._editing_draft.path if self._editing_draft else None
        return Draft(
            title=title,
            tags=list(self._draft_tags),
            body=body,
            path=path,
        )

    async def _publish(self, draft: Draft) -> None:
        """Save the draft to the wiki: create the document, attach tags
        (best-effort, capped at the server max), archive the local file, then
        open the new document."""
        client = self.app.client
        if client is None:
            return
        try:
            save_btn = self.query_one("#wiki-save-btn", Button)
        except NoMatches:
            save_btn = None
        if save_btn is not None:
            save_btn.disabled = True  # block double-submit while in flight
        # Read the chosen parent before _exit_authoring (below) tears the form down.
        parent_id = self._selected_parent_id()
        try:
            try:
                response = await client.wiki.create_document(
                    title=draft.title,
                    content=draft.body,
                    parent_document_id=parent_id,
                )
            except TissueApiError as e:
                log.warning("Wiki: publish failed: %s", e)
                self.app.notify("Couldn't save the document.", severity="error")
                return
            wiki_id = response.id
            if wiki_id is None:
                self.app.notify(
                    "The server didn't return the new document.", severity="error"
                )
                return
            tags = draft.tags[:_MAX_TAGS]
            dropped = len(draft.tags) - len(tags)
            failed: list[str] = []
            for name in tags:
                try:
                    await client.wiki.attach_tag(wiki_id, name=name)
                except TissueApiError as e:
                    log.warning("Wiki: attach tag %r failed: %s", name, e)
                    failed.append(name)
            if draft.path is not None:
                try:
                    self._draft_store().mark_synced(draft.path)
                except OSError as e:
                    log.warning("Wiki: archiving synced draft failed: %s", e)
            self._notify_published(failed, dropped)
        finally:
            if save_btn is not None:
                save_btn.disabled = False
        # Leave the editor, refresh the sidebar, then open the new document.
        self._exit_authoring()
        self._reload_drafts()
        await self._load_tree()
        await self._open_document(wiki_id)

    def _notify_published(self, failed: list[str], dropped: int) -> None:
        notes: list[str] = []
        if dropped:
            notes.append(f"{dropped} extra tag(s) skipped (max {_MAX_TAGS})")
        if failed:
            notes.append(f"couldn't attach: {', '.join(failed)}")
        if notes:
            self.app.notify(f"Document saved. ({'; '.join(notes)})", severity="warning")
        else:
            self.app.notify("Document saved to the wiki.")

    # ---- outline (table of contents) ----------------------------------
    # The outline is fed by _WikiViewer.sidebar_toc (the viewer forwards
    # TableOfContentsUpdated directly); here we only react to a TOC click.

    @on(Markdown.TableOfContentsSelected)
    def _on_toc_selected(self, message: Markdown.TableOfContentsSelected) -> None:
        message.stop()
        try:
            viewer = self.query_one("#wiki-viewer", _WikiViewer)
            block = viewer.document.query_one(f"#{message.block_id}")
        except NoMatches:
            return
        viewer.scroll_to_widget(block, top=True)

    # ---- link routing --------------------------------------------------

    @on(Markdown.LinkClicked)
    def _on_link_clicked(self, message: Markdown.LinkClicked) -> None:
        message.stop()
        href = message.href
        if href.startswith("#"):  # in-document heading anchor
            self._goto_anchor(href[1:])
            return
        parsed = _parse_link(href)
        if parsed is None:
            log.debug("Wiki: ignoring unsupported link %r", href)
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

    # ---- sidebar toggle (ctrl+b) --------------------------------------

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
