from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import (
    Button,
    DataTable,
    Footer,
    Input,
    LoadingIndicator,
    Markdown,
    OptionList,
    Select,
    Static,
    TabbedContent,
    TabPane,
    Tree,
)
from textual.widgets.option_list import Option

from tissue.api.errors import ConnectionFailed, ServerError, TissueApiError
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
from tissue.api.generated.models.wiki_snapshot_detail import WikiSnapshotDetail
from tissue.api.generated.models.wiki_snapshot_summary import WikiSnapshotSummary
from tissue.i18n.manager import i18n
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView
from tissue.widgets.wiki_editor import WikiEditor
from tissue.widgets.wiki_tree_sidebar import WikiTreeSidebar

log = logging.getLogger(__name__)

_PROJECT_TAB = "project-tab"
_WIKI_TAB = "wiki-tab"


class HomeScreen(RefreshableScreen):
    """Post-login landing screen.

    tabs: Projects | Wiki

    The Project tab is the project picker (list + detail split view).
    The Wiki tab is a directory tree (left) beside a search bar and document
    content (right).
    """

    BINDINGS = [
        Binding("ctrl+b", "toggle_wiki_tree", "wiki tree"),
    ]

    DEFAULT_CSS = """
    HomeScreen #screen-body {
        padding: 0;
    }
    HomeScreen TabbedContent {
        height: 1fr;
    }
    HomeScreen TabPane {
        height: 1fr;
        padding: 0 1;
    }
    HomeScreen .status-center {
        width: 100%;
        height: 100%;
        content-align: center middle;
        text-align: center;
        color: $text-muted;
    }
    HomeScreen .detail-body {
        width: 100%;
        height: auto;
    }
    HomeScreen .detail-title {
        width: 100%;
        text-style: bold;
        color: $text;
        padding-bottom: 1;
    }
    HomeScreen .detail-row {
        width: 100%;
        height: auto;
    }
    HomeScreen .detail-key {
        width: 14;
        color: $text-muted;
    }
    HomeScreen .detail-value {
        width: 1fr;
        color: $text;
    }
    HomeScreen .detail-desc {
        width: 100%;
        padding-top: 1;
        color: $text;
    }
    HomeScreen .detail-hint {
        width: 100%;
        padding-top: 1;
        color: $text-muted;
        text-style: italic;
    }
    HomeScreen .detail-empty {
        width: 100%;
        color: $text-muted;
    }
    /* wiki tab */
    HomeScreen #wiki-body {
        width: 100%;
        height: 1fr;
    }
    HomeScreen #wiki-main {
        width: 1fr;
        height: 1fr;
    }
    HomeScreen #wiki-actions {
        width: 100%;
        height: auto;
        margin-bottom: 1;
    }
    HomeScreen .wiki-search {
        width: 1fr;
    }
    HomeScreen #wiki-new-btn {
        width: auto;
        margin-left: 1;
    }
    HomeScreen #wiki-content {
        width: 100%;
        height: 1fr;
        padding: 1 2;
        border-title-align: left;
    }
    HomeScreen #wiki-results {
        width: 100%;
        height: auto;
    }
    HomeScreen .wiki-doc-title {
        width: 100%;
        text-style: bold;
        color: $text;
        padding-bottom: 1;
    }
    HomeScreen .wiki-meta-row {
        width: 100%;
        height: auto;
        margin: 1 0;
    }
    HomeScreen #wiki-version-select {
        width: 40;
    }
    HomeScreen .wiki-lock-badge {
        width: auto;
        padding: 0 2;
        margin-left: 2;
        color: $warning;
    }
    HomeScreen .wiki-action-row {
        width: 100%;
        height: auto;
        margin-bottom: 1;

        Button {
            margin-right: 1;
            min-width: 10;
        }
    }
    HomeScreen .wiki-version-banner {
        width: 100%;
        padding: 0 1;
        color: $warning;
        text-style: italic;
    }
    HomeScreen #wiki-doc-body {
        width: 100%;
        height: auto;
    }
    HomeScreen .wiki-search-header {
        width: 100%;
        color: $text-muted;
        padding-bottom: 1;
    }
    HomeScreen .wiki-muted {
        width: 100%;
        color: $text-muted;
        padding: 1 0;
    }
    """

    def __init__(self) -> None:
        super().__init__()
        # project tab state
        self._projects: list[ProjectSummary] | None = None
        self._project_error: str | None = None
        self._loading = False
        self._empty_prompt_shown = False
        # wiki tab state
        self._wiki_nodes: list[WikiDocumentTreeNode] | None = None
        self._wiki_error: str | None = None
        self._wiki_loading = False
        self._wiki_requested = False
        self._wiki_doc: WikiDocumentDetail | None = None
        self._wiki_versions: list[WikiSnapshotSummary] | None = None
        self._wiki_snapshot: WikiSnapshotDetail | None = None
        self._wiki_results: list[WikiDocumentSearchResult] | None = None
        self._wiki_total = 0
        self._wiki_query = ""
        # "empty" | "doc" | "search" | "error" | "create" | "edit"
        self._wiki_view = "empty"
        self._wiki_content_error: str | None = None
        self._wiki_saving = False
        self._wiki_create_parent_id: int | None = None
        self._wiki_create_parent_title: str | None = None
        self._wiki_sidebar_visible = True
        # tab tracking
        self._active_tab = _PROJECT_TAB

    def compose(self) -> ComposeResult:
        with Container(id="screen-body"):
            with TabbedContent(initial=self._active_tab, id="home-tabs"):
                with TabPane(i18n.get("home_tab_project"), id=_PROJECT_TAB):
                    yield from self._compose_project_tab()
                with TabPane(i18n.get("home_tab_wiki"), id=_WIKI_TAB):
                    yield from self._compose_wiki_tab()
        yield Footer()

    def _compose_project_tab(self) -> ComposeResult:
        if self._project_error is not None:
            yield Static(self._project_error, classes="status-center")
        elif self._projects is None:
            yield LoadingIndicator()
        else:
            yield TableDetailSplitView(
                columns=[
                    Column("key", i18n.get("project_col_key"), 12),
                    Column("title", i18n.get("project_col_title")),
                    Column("visibility", i18n.get("project_col_visibility"), 12),
                    Column("updated", i18n.get("project_col_updated"), 16),
                ],
                row_builder=self._row,
                detail_renderer=self._render_detail,
                items=self._projects,
                id="project-split",
                table_title=i18n.get("project_list_title"),
                detail_title=i18n.get("project_detail_title"),
            )
            if self._active_tab == _PROJECT_TAB:
                self.call_after_refresh(self._focus_table)

    def _compose_wiki_tab(self) -> ComposeResult:
        with Horizontal(id="wiki-body"):
            if self._wiki_sidebar_visible:
                yield self._build_wiki_sidebar()
            with Vertical(id="wiki-main"):
                with Horizontal(id="wiki-actions"):
                    yield Input(
                        placeholder=i18n.get("wiki_search_placeholder"),
                        id="wiki-search",
                        classes="wiki-search",
                    )
                    yield Button(
                        i18n.get("wiki_new_btn"),
                        id="wiki-new-btn",
                        classes="-btn-success",
                    )
                content = VerticalScroll(id="wiki-content", classes="panel")
                content.border_title = self._wiki_content_title()
                with content:
                    yield from self._wiki_content_widgets()

    def _build_wiki_sidebar(self) -> WikiTreeSidebar:
        return WikiTreeSidebar(self._wiki_nodes, error=self._wiki_error)

    def _wiki_content_title(self) -> str:
        if self._wiki_view == "create":
            return i18n.get("wiki_create_title")
        if self._wiki_view == "edit":
            return i18n.get("wiki_edit_title")
        return i18n.get("home_tab_wiki")

    def _wiki_content_widgets(self) -> ComposeResult:
        if self._wiki_view == "error":
            yield Static(
                self._wiki_content_error or i18n.get("wiki_doc_error"),
                classes="wiki-muted",
            )
        elif self._wiki_view == "search":
            results = self._wiki_results or []
            shown = [r for r in results if r.id is not None]
            total = self._wiki_total or len(shown)
            yield Static(
                i18n.get("wiki_search_results", count=total, query=self._wiki_query),
                classes="wiki-search-header",
            )
            if not shown:
                yield Static(i18n.get("wiki_search_empty"), classes="wiki-muted")
            else:
                if len(shown) < total:  # only the first page is listed
                    yield Static(
                        i18n.get("wiki_search_truncated", shown=len(shown)),
                        classes="wiki-muted",
                    )
                yield OptionList(
                    *(self._result_option(r) for r in shown), id="wiki-results"
                )
        elif self._wiki_view == "create":
            yield self._build_wiki_editor("create")
        elif self._wiki_view == "edit" and self._wiki_doc is not None:
            yield self._build_wiki_editor("edit")
        elif self._wiki_view == "doc" and self._wiki_doc is not None:
            yield from self._wiki_doc_header(self._wiki_doc)
            yield Container(*self._wiki_doc_body_widgets(), id="wiki-doc-body")
        else:
            yield Static(i18n.get("wiki_content_empty"), classes="wiki-muted")

    def _wiki_doc_header(self, doc: WikiDocumentDetail) -> ComposeResult:
        yield Static(doc.title or "-", markup=False, classes="wiki-doc-title")
        yield detail_row(
            i18n.get("wiki_doc_parent_label"),
            doc.parent_document_title or i18n.get("wiki_doc_parent_none"),
        )
        options: list[tuple[str, str]] = [
            (
                i18n.get("wiki_version_current", version=doc.current_version or "-"),
                "current",
            )
        ]
        for v in self._wiki_versions or []:
            label = f"{v.snapshot_version or '-'} · {v.update_type or '?'}"
            options.append((label, str(v.id)))
        value = "current"
        if self._wiki_snapshot is not None and self._wiki_snapshot.id is not None:
            sid = str(self._wiki_snapshot.id)
            if any(opt_value == sid for _, opt_value in options):
                value = sid
        meta: list = [
            Select(options, value=value, allow_blank=False, id="wiki-version-select")
        ]
        if doc.locked:
            meta.append(
                Static(i18n.get("wiki_locked_badge"), classes="wiki-lock-badge")
            )
        yield Horizontal(*meta, classes="wiki-meta-row")
        yield Horizontal(
            Button(
                i18n.get("wiki_edit_btn"),
                id="wiki-edit-btn",
                disabled=bool(doc.locked),
            ),
            Button(
                i18n.get("wiki_unlock_btn")
                if doc.locked
                else i18n.get("wiki_lock_btn"),
                id="wiki-lock-btn",
            ),
            classes="wiki-action-row",
        )

    def _wiki_doc_body_widgets(self) -> ComposeResult:
        snap = self._wiki_snapshot
        if snap is not None:
            yield Static(
                i18n.get("wiki_version_viewing", version=snap.snapshot_version or "-"),
                classes="wiki-version-banner",
            )
            yield Markdown(snap.snapshot_content or "")
        else:
            doc = self._wiki_doc
            yield Markdown((doc.content if doc else "") or "")

    def _build_wiki_editor(self, mode: str) -> WikiEditor:
        if mode == "edit" and self._wiki_doc is not None:
            return WikiEditor(
                mode="edit",
                title=self._wiki_doc.title or "",
                content=self._wiki_doc.content or "",
            )
        has_parent = self._wiki_create_parent_id is not None
        return WikiEditor(
            mode="create",
            parent_title=self._wiki_create_parent_title,
            allow_child=has_parent,
            allow_parent=has_parent,
        )

    _SEARCH_WINDOW = 50  # chars shown on each side of a content match
    _SEARCH_LEAD = 100  # when the match was in the title

    def _result_option(self, result: WikiDocumentSearchResult) -> Option:
        words = [w for w in self._wiki_query.split() if w]
        label = Text()
        # Title is always shown
        self._append_highlighted(
            label, result.title or "-", words, "bold", "bold reverse"
        )
        preview = self._search_preview(result.content_snippet or "", words)
        if preview:
            label.append("\n  ")
            self._append_highlighted(label, preview, words, "dim", "bold reverse")
        return Option(label, id=str(result.id) if result.id is not None else None)

    @classmethod
    def _search_preview(cls, snippet: str, words: list[str]) -> str:
        """A window for the result snippet."""
        if not snippet:
            return ""
        lower = snippet.lower()
        first: int | None = None
        span = 0
        for w in words:
            i = lower.find(w.lower())
            if i >= 0 and (first is None or i < first):
                first, span = i, len(w)
        if first is not None:
            start = max(0, first - cls._SEARCH_WINDOW)
            end = min(len(snippet), first + span + cls._SEARCH_WINDOW)
            prefix = "…" if start > 0 else ""
            suffix = "…" if end < len(snippet) else ""
            return f"{prefix}{snippet[start:end]}{suffix}"
        head = snippet[: cls._SEARCH_LEAD]
        return head + ("…" if len(snippet) > cls._SEARCH_LEAD else "")

    @staticmethod
    def _append_highlighted(
        text: Text, value: str, words: list[str], base: str, match: str
    ) -> None:
        spans: list[tuple[int, int]] = []
        lower = value.lower()
        for w in words:
            needle = w.lower()
            if not needle:
                continue
            start = 0
            while True:
                i = lower.find(needle, start)
                if i < 0:
                    break
                spans.append((i, i + len(needle)))
                start = i + len(needle)
        if not spans:
            text.append(value, style=base)
            return
        spans.sort()
        merged: list[tuple[int, int]] = []
        for s, e in spans:
            if merged and s <= merged[-1][1]:
                merged[-1] = (merged[-1][0], max(merged[-1][1], e))
            else:
                merged.append((s, e))
        pos = 0
        for s, e in merged:
            if s > pos:
                text.append(value[pos:s], style=base)
            text.append(value[s:e], style=match)
            pos = e
        if pos < len(value):
            text.append(value[pos:], style=base)

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    async def refresh_data(self) -> None:
        if self._active_tab == _WIKI_TAB:
            await self._load_wiki_tree()
        else:
            await self._load_projects()

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "toggle_wiki_tree":
            return self._active_tab == _WIKI_TAB
        return super().check_action(action, parameters)

    @on(TabbedContent.TabActivated)
    def _on_tab_activated(self, event: TabbedContent.TabActivated) -> None:
        self._active_tab = event.tabbed_content.active
        self.refresh_bindings()  # show/hide the ctrl+b binding in the footer
        if self._active_tab == _WIKI_TAB and not self._wiki_requested:
            self._wiki_requested = True
            self.run_worker(
                self._load_wiki_tree(), exclusive=True, group="wiki-tree-load"
            )

    def action_toggle_wiki_tree(self) -> None:
        if self._active_tab != _WIKI_TAB:
            return
        if self._wiki_sidebar_visible:
            try:
                self.query_one(WikiTreeSidebar).remove()
            except NoMatches:
                pass
            self._wiki_sidebar_visible = False
            return
        try:
            body = self.query_one("#wiki-body")
        except NoMatches:
            return
        body.mount(self._build_wiki_sidebar(), before=0)
        self._wiki_sidebar_visible = True

    async def _load_projects(self) -> None:
        client = self.app.client
        if client is None:
            log.error("Home load attempted but TissueClient is not set")
            return
        if self._loading:
            return
        self._loading = True
        try:
            page = await client.projects.list_projects(size=100)
        except ConnectionFailed:
            self._set_project_error(i18n.get("project_list_error_unreachable"))
            return
        except ServerError:
            self._set_project_error(i18n.get("project_list_error_server"))
            return
        except TissueApiError as e:
            log.warning("Failed to load projects: %s", e)
            self._set_project_error(i18n.get("project_list_error_generic"))
            return
        finally:
            self._loading = False

        self._project_error = None
        self._projects = list(page.content or [])
        self.refresh(recompose=True)
        if not self._projects:
            self.call_after_refresh(self._prompt_create_if_empty)

    def _set_project_error(self, message: str) -> None:
        self._project_error = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    def _row(self, idx: int, project: ProjectSummary) -> list[str | Text]:
        return [
            project.key or "-",
            Text(project.title or "-"),
            self._visibility_label(project.visibility),
            format_relative(project.last_updated_at),
        ]

    def _render_detail(
        self, project: ProjectSummary | None, content: Container, actions: Container
    ) -> None:
        content.remove_children()
        if project is None:
            content.mount(
                Static(i18n.get("project_detail_empty"), classes="detail-empty")
            )
        else:
            content.mount(self._build_detail(project))
        if not actions.children:
            actions.mount(
                Button(
                    i18n.get("project_create_btn"),
                    id="project-create-btn",
                    classes="-btn-success",
                )
            )

    def _build_detail(self, project: ProjectSummary) -> Vertical:
        return Vertical(
            Static(project.title or "-", markup=False, classes="detail-title"),
            detail_row(i18n.get("project_col_key"), project.key or "-"),
            detail_row(
                i18n.get("project_col_visibility"),
                self._visibility_label(project.visibility),
            ),
            detail_row(
                i18n.get("project_field_created"), format_relative(project.created_at)
            ),
            detail_row(
                i18n.get("project_col_updated"),
                format_relative(project.last_updated_at),
            ),
            Static(
                project.description or i18n.get("project_no_description"),
                markup=False,
                classes="detail-desc",
            ),
            Static(i18n.get("project_open_hint"), classes="detail-hint"),
            classes="detail-body",
        )

    def _focus_table(self) -> None:
        if self._active_tab != _PROJECT_TAB:  # user switched tabs before refresh
            return
        try:
            self.query_one("#project-split").query_one(DataTable).focus()
        except NoMatches:
            pass

    @on(DataTable.RowSelected)
    def _on_row_selected(self, event: DataTable.RowSelected) -> None:
        idx = event.cursor_row
        if self._projects and 0 <= idx < len(self._projects):
            self._open_project(self._projects[idx].key)

    def _open_project(self, project_key: str | None) -> None:
        if not project_key:
            return
        from tissue.screens.project_home import ProjectHomeScreen

        title = next(
            (p.title for p in (self._projects or []) if p.key == project_key), None
        )
        self.app.push_screen(ProjectHomeScreen(project_key, title=title))

    @on(Button.Pressed, "#project-create-btn")
    def _on_create_button(self) -> None:
        self._open_create_modal()

    def _prompt_create_if_empty(self) -> None:
        if self._projects:
            return
        if self._empty_prompt_shown:  # first-run nudge only, not on every refresh
            return
        if self.app.screen is not self:  # a modal is already on top
            return
        self._empty_prompt_shown = True
        from tissue.screens.empty_projects_modal import EmptyProjectsModal

        self.app.push_screen(EmptyProjectsModal(), self._on_empty_choice)

    def _on_empty_choice(self, create: bool | None) -> None:
        if create:
            self._open_create_modal()

    def _open_create_modal(self) -> None:
        from tissue.screens.create_project_modal import CreateProjectModal

        self.app.push_screen(CreateProjectModal(), self._on_project_created)

    def _on_project_created(self, created_key: str | None) -> None:
        if created_key:
            self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        key = f"project_visibility_{visibility.lower()}"
        label = i18n.get(key)
        if label == key:  # unknown enum → readable fallback
            return visibility.replace("_", " ").title()
        return label

    # ---- wiki tab: data -------------------------------------------------

    async def _load_wiki_tree(self) -> None:
        client = self.app.client
        if client is None:
            return
        if self._wiki_loading:
            return
        self._wiki_loading = True
        try:
            nodes = await client.wiki.get_tree()
        except ConnectionFailed:
            self._wiki_error = i18n.get("wiki_error_unreachable")
            self.refresh(recompose=True)
            return
        except ServerError:
            self._wiki_error = i18n.get("wiki_error_server")
            self.refresh(recompose=True)
            return
        except TissueApiError as e:
            log.warning("Failed to load wiki tree: %s", e)
            self._wiki_error = i18n.get("wiki_error_generic")
            self.refresh(recompose=True)
            return
        finally:
            self._wiki_loading = False

        self._wiki_error = None
        self._wiki_nodes = list(nodes or [])
        # Full recompose rebuilds the tree from cache; rare (lazy load + manual
        # refresh only), so the lost expand state is acceptable.
        self.refresh(recompose=True)

    async def _load_wiki_doc(self, wiki_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            doc = await client.wiki.get_document(wiki_id)
        except TissueApiError as e:
            log.warning("Failed to load wiki document %s: %s", wiki_id, e)
            self._wiki_view = "error"
            self._wiki_content_error = i18n.get("wiki_doc_error")
            await self._render_wiki_content()
            return
        self._wiki_doc = doc
        self._wiki_snapshot = None
        self._wiki_view = "doc"
        try:  # best-effort: the version dropdown is non-essential
            self._wiki_versions = await client.wiki.list_versions(wiki_id)
        except TissueApiError as e:
            log.debug("Failed to load wiki versions for %s: %s", wiki_id, e)
            self._wiki_versions = None
        await self._render_wiki_content()

    async def _load_wiki_snapshot(self, snapshot_id: int) -> None:
        client = self.app.client
        doc = self._wiki_doc
        if client is None or doc is None or doc.id is None:
            return
        try:
            snap = await client.wiki.get_version(doc.id, snapshot_id)
        except TissueApiError as e:
            log.warning("Failed to load wiki snapshot %s: %s", snapshot_id, e)
            self.app.notify(i18n.get("wiki_version_error"), severity="error")
            return
        self._wiki_snapshot = snap
        await self._render_wiki_doc_body()

    async def _toggle_lock(self, wiki_id: int, lock: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if lock:
                await client.wiki.lock(wiki_id)
            else:
                await client.wiki.unlock(wiki_id)
        except TissueApiError as e:
            verb = "lock" if lock else "unlock"
            log.warning("Failed to %s wiki %s: %s", verb, wiki_id, e)
            self.app.notify(i18n.get("wiki_lock_failed"), severity="error")
            return
        self.app.notify(
            i18n.get("wiki_lock_success" if lock else "wiki_unlock_success")
        )
        await self._load_wiki_doc(wiki_id)

    async def _do_wiki_search(self, query: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.wiki.search(keyword=query)
        except TissueApiError as e:
            log.warning("Wiki search failed: %s", e)
            self._wiki_view = "error"
            self._wiki_content_error = i18n.get("wiki_search_error")
            await self._render_wiki_content()
            return
        self._wiki_results = list(page.content or [])
        self._wiki_total = (
            page.total_elements
            if page.total_elements is not None
            else len(self._wiki_results)
        )
        self._wiki_view = "search"
        await self._render_wiki_content()

    async def _render_wiki_content(self) -> None:
        """Targeted update of just the content pane (keeps the tree intact).

        The removal is awaited before mounting: the new tree reuses fixed ids
        (e.g. #wiki-doc-body), so mounting before the old nodes are gone raises
        DuplicateIds.
        """
        try:
            content = self.query_one("#wiki-content", VerticalScroll)
        except NoMatches:
            return
        content.border_title = self._wiki_content_title()
        await content.remove_children()
        await content.mount_all(list(self._wiki_content_widgets()))

    async def _render_wiki_doc_body(self) -> None:
        """Swap only the rendered body (current vs a version snapshot)."""
        try:
            body = self.query_one("#wiki-doc-body", Container)
        except NoMatches:
            return
        await body.remove_children()
        await body.mount_all(list(self._wiki_doc_body_widgets()))

    # ---- wiki tab: events ----------------------------------------------

    @on(Tree.NodeSelected)
    def _on_wiki_node_selected(self, event: Tree.NodeSelected) -> None:
        data = event.node.data
        if not isinstance(data, int):
            return
        self.run_worker(
            self._load_wiki_doc(data), exclusive=True, group="wiki-doc-load"
        )

    @on(Input.Submitted, "#wiki-search")
    async def _on_wiki_search_submitted(self, event: Input.Submitted) -> None:
        query = event.value.strip()
        if not query:
            self._wiki_results = None
            self._wiki_query = ""
            self._wiki_view = "doc" if self._wiki_doc is not None else "empty"
            await self._render_wiki_content()
            return
        self._wiki_query = query
        self.run_worker(
            self._do_wiki_search(query), exclusive=True, group="wiki-search"
        )

    @on(OptionList.OptionSelected, "#wiki-results")
    def _on_wiki_result_selected(self, event: OptionList.OptionSelected) -> None:
        option_id = event.option.id
        if option_id and option_id.isdigit():
            self.run_worker(
                self._load_wiki_doc(int(option_id)),
                exclusive=True,
                group="wiki-doc-load",
            )

    @on(Button.Pressed, "#wiki-new-btn")
    async def _on_wiki_new(self) -> None:
        # The detail pane becomes the create form (no modal). Seed the parent
        # context from the document currently open so the form can offer
        # child/parent placement relative to it.
        if self._wiki_view == "doc" and self._wiki_doc is not None:
            self._wiki_create_parent_id = self._wiki_doc.id
            self._wiki_create_parent_title = self._wiki_doc.title
        else:
            self._wiki_create_parent_id = None
            self._wiki_create_parent_title = None
        self._wiki_view = "create"
        await self._render_wiki_content()

    @on(Select.Changed, "#wiki-version-select")
    async def _on_wiki_version_changed(self, event: Select.Changed) -> None:
        value = event.value
        if value == "current" or value is Select.BLANK:
            if self._wiki_snapshot is not None:
                self._wiki_snapshot = None
                await self._render_wiki_doc_body()
            return
        if isinstance(value, str) and value.isdigit():
            self.run_worker(
                self._load_wiki_snapshot(int(value)),
                exclusive=True,
                group="wiki-version",
            )

    @on(Button.Pressed, "#wiki-edit-btn")
    async def _on_wiki_edit(self) -> None:
        if self._wiki_doc is None or self._wiki_doc.locked:
            return
        self._wiki_view = "edit"
        await self._render_wiki_content()

    @on(Button.Pressed, "#wiki-lock-btn")
    def _on_wiki_lock(self) -> None:
        doc = self._wiki_doc
        if doc is None or doc.id is None:
            return
        self.run_worker(
            self._toggle_lock(doc.id, not bool(doc.locked)),
            exclusive=True,
            group="wiki-lock",
        )

    @on(WikiEditor.Cancelled)
    async def _on_wiki_editor_cancelled(self) -> None:
        self._wiki_view = "doc" if self._wiki_doc is not None else "empty"
        await self._render_wiki_content()

    @on(WikiEditor.Saved)
    def _on_wiki_editor_saved(self, event: WikiEditor.Saved) -> None:
        if self._wiki_saving:
            return
        self._wiki_saving = True
        self.run_worker(self._do_wiki_save(event), exclusive=True, group="wiki-save")

    async def _do_wiki_save(self, event: WikiEditor.Saved) -> None:
        client = self.app.client
        if client is None:
            self._wiki_saving = False
            return
        creating = self._wiki_view == "create"
        try:
            if creating:
                target_id = await self._create_from_editor(client, event)
                self.app.notify(i18n.get("wiki_create_success", title=event.title))
            else:
                doc = self._wiki_doc
                if doc is None or doc.id is None:
                    self._wiki_saving = False
                    return
                if event.title != doc.title:
                    await client.wiki.update_title(doc.id, title=event.title)
                if event.content != doc.content:
                    await client.wiki.update_content(
                        doc.id,
                        content=event.content,
                        version_update_type=event.version_update_type or "PATCH",
                        edit_reason=event.edit_reason,
                    )
                target_id = doc.id
                self.app.notify(i18n.get("wiki_edit_success"))
        except TissueApiError as e:
            log.warning("Wiki save failed: %s", e)
            self._wiki_saving = False
            self.app.notify(
                i18n.get(
                    "wiki_create_failed" if creating else "wiki_edit_failed",
                    reason=self._wiki_failure_reason(e),
                ),
                severity="error",
            )
            return

        self._wiki_saving = False
        if target_id is not None:
            await self._load_wiki_doc(target_id)
            await self._load_wiki_tree()
        else:
            self._wiki_view = "doc" if self._wiki_doc is not None else "empty"
            await self._render_wiki_content()

    async def _create_from_editor(self, client, event: WikiEditor.Saved) -> int | None:
        mode = event.create_mode or "top"
        current = self._wiki_doc
        if mode == "child" and current is not None:
            parent_id = current.id
        elif mode == "parent" and current is not None:
            # the new doc takes the current doc's place under its grandparent...
            parent_id = current.parent_document_id
        else:
            parent_id = None
        response = await client.wiki.create_document(
            title=event.title, content=event.content, parent_document_id=parent_id
        )
        new_id = response.id
        if (
            mode == "parent"
            and current is not None
            and current.id is not None
            and new_id is not None
        ):
            # ...then the current doc becomes a child of the new one. The
            # document already exists at this point, so a reparent failure must
            # not discard it — surface it as a partial result and keep going.
            try:
                await client.wiki.set_parent(current.id, parent_document_id=new_id)
            except TissueApiError as e:
                log.warning("Wiki created but set_parent failed: %s", e)
                self.app.notify(
                    i18n.get("wiki_create_reparent_failed"), severity="warning"
                )
        return new_id

    @staticmethod
    def _wiki_failure_reason(exc: TissueApiError) -> str:
        return exc.detail or exc.title or str(exc)
