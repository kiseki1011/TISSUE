from __future__ import annotations

import logging

from rich.markup import escape
from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import (
    Button,
    Checkbox,
    DataTable,
    Input,
    Label,
    LoadingIndicator,
    Markdown,
    OptionList,
    Rule,
    Select,
    Static,
    TabbedContent,
    TabPane,
    Tree,
)
from textual.widgets.option_list import Option
from textual.widgets.tree import TreeNode

from tissue.api.errors import ConnectionFailed, ServerError, TissueApiError
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.api.generated.models.wiki_document_tree_node import WikiDocumentTreeNode
from tissue.api.generated.models.wiki_snapshot_detail import WikiSnapshotDetail
from tissue.api.generated.models.wiki_snapshot_summary import WikiSnapshotSummary
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.search_bar import SearchBar
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView
from tissue.widgets.text_button import TextButton
from tissue.widgets.wiki_editor import WikiEditor
from tissue.widgets.wiki_tree_sidebar import WikiTreeSidebar

log = logging.getLogger(__name__)

_PROJECT_TAB = "project-tab"
_WIKI_TAB = "wiki-tab"


class DeprecatedHomeScreen(RefreshableScreen):
    """Deprecated tabbed home (Projects | Wiki), kept for reference only.

    Superseded by the dashboard `HomeScreen` in home.py; not routed anywhere.
    The Projects/Wiki tabs were extracted into standalone ProjectListScreen /
    WikiScreen.
    """

    BINDINGS = [
        Binding("ctrl+b", "toggle_wiki_tree", "wiki tree"),
    ]

    CSS_PATH = "home_deprecated.tcss"

    def __init__(self) -> None:
        super().__init__()
        # project tab state
        self._projects: list[ProjectSummary] | None = None
        self._project_error: str | None = None
        self._project_query = ""
        self._include_archived = False
        self._selected_project: ProjectSummary | None = None
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

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            with TabbedContent(initial=self._active_tab, id="home-tabs"):
                with TabPane("Projects", id=_PROJECT_TAB):
                    yield from self._compose_project_tab()
                with TabPane("Wiki", id=_WIKI_TAB):
                    yield from self._compose_wiki_tab()

    def _compose_project_tab(self) -> ComposeResult:
        if self._project_error is not None:
            yield self._project_status_panel(
                Static(self._project_error, classes="status-error")
            )
            return
        if self._projects is None:
            yield self._project_status_panel(LoadingIndicator())
            return
        # Truly empty (no projects AND no active search/filter) → first-run CTA.
        no_filter = not self._project_query and not self._include_archived
        if not self._projects and no_filter:
            yield self._project_status_panel(
                Static("No projects yet", classes="status-title"),
                Static(
                    "You don't have any projects yet. Create your first one?",
                    classes="status-sub",
                ),
                Button(
                    "+ New project",
                    id="project-create-btn",
                    classes="-btn-success",
                ),
            )
            return
        with Vertical(id="project-main"):
            toggle = Checkbox(
                "Archived",
                value=self._include_archived,
                id="project-archived-toggle",
            )
            yield SearchBar(
                toggle,
                input_id="project-search",
                placeholder="Search projects…",
                value=self._project_query,
            )
            if not self._projects:
                yield Static("No matching projects.", classes="detail-empty")
            else:
                yield TableDetailSplitView(
                    columns=[
                        Column("status", "", 3),
                        Column("key", "Key", 12),
                        Column("title", "Title"),
                        Column("visibility", "Visibility", 12),
                        Column("archived", "Archived", 10),
                    ],
                    row_builder=self._row,
                    detail_renderer=self._render_detail,
                    items=self._projects,
                    id="project-split",
                    table_title="Projects",
                    detail_title="Details",
                )
        if self._active_tab == _PROJECT_TAB:
            if self._projects:
                self.call_after_refresh(self._focus_table)
            else:  # no matches — keep focus in the search box to refine/clear
                self.call_after_refresh(self._focus_project_search)

    def _project_status_panel(self, *children: Widget) -> Vertical:
        """Bordered panel for the non-loaded project states (loading / error /
        empty), so chrome stays consistent with the loaded split view."""
        panel = Vertical(*children, classes="panel project-status")
        panel.border_title = "Projects"
        return panel

    def _compose_wiki_tab(self) -> ComposeResult:
        with Horizontal(id="wiki-body"):
            if self._wiki_sidebar_visible:
                yield self._build_wiki_sidebar()
            with Vertical(id="wiki-main"):
                yield SearchBar(
                    Button(
                        "+ New document",
                        id="wiki-new-btn",
                        classes="-btn-success",
                    ),
                    input_id="wiki-search",
                    placeholder="Search wiki…",
                )
                content = VerticalScroll(id="wiki-content", classes="panel")
                content.border_title = self._wiki_content_title()
                with content:
                    # Inner pane holds the padded content so the scrollbar lives
                    # on the (unpadded) outer edge, not inset by the padding.
                    with Container(id="wiki-content-inner"):
                        yield from self._wiki_content_widgets()

    def _build_wiki_sidebar(self) -> WikiTreeSidebar:
        return WikiTreeSidebar(self._wiki_nodes, error=self._wiki_error)

    def _wiki_content_title(self) -> str:
        if self._wiki_view == "create":
            return "New document"
        if self._wiki_view == "edit":
            return "Edit document"
        return "Wiki"

    def _wiki_content_widgets(self) -> ComposeResult:
        if self._wiki_view == "error":
            yield Static(
                self._wiki_content_error or "Failed to load the document.",
                classes="wiki-muted",
            )
        elif self._wiki_view == "search":
            results = self._wiki_results or []
            shown = [r for r in results if r.id is not None]
            total = self._wiki_total or len(shown)
            yield Static(
                f"{total} result(s) for '{self._wiki_query}'",
                classes="wiki-search-header",
            )
            if not shown:
                yield Static("No matching documents.", classes="wiki-muted")
            else:
                if len(shown) < total:  # only the first page is listed
                    yield Static(
                        f"Showing the top {len(shown)}.",
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
            yield Rule(classes="wiki-divider")
            yield Container(*self._wiki_doc_body_widgets(), id="wiki-doc-body")
        elif self._wiki_error and not self._wiki_sidebar_visible:
            # Tree (and its error) hidden via ctrl+b — surface the load failure
            # here so it isn't invisible, without forcing the sidebar back.
            yield Static(self._wiki_error, classes="wiki-muted")
        else:
            yield Static("Select a document to view its content.", classes="wiki-muted")

    def _wiki_doc_header(self, doc: WikiDocumentDetail) -> ComposeResult:
        # Left column: title + parent (+ lock badge). Right column (top-right):
        # the version picker, with the edit/lock actions beneath it.
        left: list[Widget] = [
            Static(doc.title or "-", markup=False, classes="wiki-doc-title"),
            self._wiki_parent_row(doc),
            detail_row("Locked", "🔒" if doc.locked else "-"),
        ]
        options: list[tuple[str, str]] = [
            (f"Current ({doc.current_version or '-'})", "current")
        ]
        for v in self._wiki_versions or []:
            label = f"{v.snapshot_version or '-'} · {v.update_type or '?'}"
            options.append((label, str(v.id)))
        value = "current"
        if self._wiki_snapshot is not None and self._wiki_snapshot.id is not None:
            sid = str(self._wiki_snapshot.id)
            if any(opt_value == sid for _, opt_value in options):
                value = sid
        right = Vertical(
            Select(options, value=value, allow_blank=False, id="wiki-version-select"),
            Horizontal(
                TextButton("Edit", id="wiki-edit-btn", disabled=bool(doc.locked)),
                TextButton("Unlock" if doc.locked else "Lock", id="wiki-lock-btn"),
                classes="wiki-header-buttons",
            ),
            classes="wiki-header-right",
        )
        yield Horizontal(
            Vertical(*left, classes="wiki-header-left"),
            right,
            classes="wiki-doc-header",
        )

    def _wiki_parent_row(self, doc: WikiDocumentDetail) -> Horizontal:
        """Parent row; the title links to that document when one exists."""
        title = doc.parent_document_title
        if doc.parent_document_id is not None and title:
            # Content markup click-action: clicking the title navigates in place.
            value: Widget = Static(
                f"[@click=screen.open_wiki_parent]{escape(title)}[/]",
                classes="detail-value wiki-parent-link",
            )
        else:
            value = Label(Text("root"), classes="detail-value")
        return Horizontal(
            Label("Parent:", classes="detail-key"),
            value,
            classes="detail-row",
        )

    def _wiki_doc_body_widgets(self) -> ComposeResult:
        snap = self._wiki_snapshot
        if snap is not None:
            yield Static(
                self._snapshot_banner_content(snap),
                classes="wiki-version-banner",
            )
            yield Markdown(snap.snapshot_content or "")
        else:
            doc = self._wiki_doc
            yield Markdown((doc.content if doc else "") or "")

    @staticmethod
    def _snapshot_banner_content(snap: WikiSnapshotDetail) -> Text:
        content = Text(f"Viewing version {snap.snapshot_version or '-'} (read-only)")
        if snap.edit_reason:
            # Dim the reason line so it reads as secondary to the version notice.
            content.append(f"\nChange reason: {snap.edit_reason}", style="dim")
        return content

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

    def top_bar_breadcrumb(self) -> str:
        return "Home"

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    async def refresh_data(self) -> None:
        if self._active_tab == _WIKI_TAB:
            await self._load_wiki_tree()
        else:
            await self._load_projects()

    def can_refresh(self) -> bool:
        # An in-progress wiki editor must survive 'r': refresh recomposes the
        # content pane, which would silently discard the unsaved draft. Suppress
        # the binding while creating/editing (also hides it from the footer).
        return not (
            self._active_tab == _WIKI_TAB and self._wiki_view in ("create", "edit")
        )

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
            # Focus search only on the first open; on re-entry leave focus where
            # the user left it (tree/doc) so we don't trap them in a text field.
            self.call_after_refresh(self._focus_wiki_search)

    def _focus_wiki_search(self) -> None:
        if self._active_tab != _WIKI_TAB:  # user switched away before refresh
            return
        try:
            self.query_one("#wiki-search", Input).focus()
        except NoMatches:
            pass

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
        # No reentrancy flag: callers launch this as an exclusive worker
        # (group "project-load"), which cancels any in-flight load so the newest
        # query/filter always wins.
        try:
            page = await client.projects.list_projects(
                size=100,
                keyword=self._project_query or None,
                include_archived=self._include_archived,
            )
        except ConnectionFailed:
            self._set_project_error("Cannot reach server. Press r to retry.")
            return
        except ServerError:
            self._set_project_error("Server error. Press r to retry.")
            return
        except TissueApiError as e:
            log.warning("Failed to load projects: %s", e)
            self._set_project_error("Failed to load projects. Press r to retry.")
            return

        self._project_error = None
        self._projects = list(page.content or [])
        self._sort_projects()
        self.refresh(recompose=True)

    def _set_project_error(self, message: str) -> None:
        self._project_error = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    def _row(self, idx: int, project: ProjectSummary) -> list[str | Text]:
        archived = bool(project.archived)
        pin = "📌" if self._is_pinned(project.key) else ""
        key = project.key or "-"
        title = project.title or "-"
        visibility = self._visibility_label(project.visibility)
        archived_label = "ARCHIVED" if archived else "-"
        if archived:  # de-emphasize the whole row
            return [
                Text(pin, style="dim"),
                Text(key, style="dim"),
                Text(title, style="dim"),
                Text(visibility, style="dim"),
                Text(archived_label, style="dim"),
            ]
        return [pin, key, Text(title), visibility, archived_label]

    def _render_detail(
        self, project: ProjectSummary | None, content: Container, actions: Container
    ) -> None:
        self._selected_project = project
        content.remove_children()
        if project is None:
            content.mount(Static("No project selected.", classes="detail-empty"))
        else:
            content.mount(self._build_detail(project))
        # Buttons are mounted once (stable ids); per-row state is synced below to
        # avoid remove/mount churn (and DuplicateIds) on every row highlight.
        if not actions.children:
            actions.mount(
                TextButton("+ New project", id="project-create-btn"),
                TextButton("Pin", id="project-pin-btn"),
                TextButton("Archive", id="project-archive-btn"),
            )
        self._sync_project_action_buttons(project)

    def _sync_project_action_buttons(self, project: ProjectSummary | None) -> None:
        try:
            pin_btn = self.query_one("#project-pin-btn", Button)
            arch_btn = self.query_one("#project-archive-btn", Button)
        except NoMatches:
            return
        if project is None or not project.key:
            pin_btn.disabled = True
            arch_btn.disabled = True
            return
        pin_btn.disabled = False
        arch_btn.disabled = False
        pin_btn.label = "Unpin" if self._is_pinned(project.key) else "Pin"
        arch_btn.label = "Unarchive" if project.archived else "Archive"

    def _build_detail(self, project: ProjectSummary) -> Vertical:
        return Vertical(
            Static(project.title or "-", markup=False, classes="detail-title"),
            detail_row("Key", project.key or "-"),
            detail_row(
                "Visibility",
                self._visibility_label(project.visibility),
            ),
            detail_row("Created", format_relative(project.created_at)),
            detail_row(
                "Updated",
                format_relative(project.last_updated_at),
            ),
            Static(
                project.description or "No description.",
                markup=False,
                classes="detail-desc",
            ),
            Static("Press Enter to open", classes="detail-hint"),
            classes="detail-body",
        )

    def _focus_table(self) -> None:
        if self._active_tab != _PROJECT_TAB:  # user switched tabs before refresh
            return
        try:
            self.query_one("#project-split").query_one(DataTable).focus()
        except NoMatches:
            pass

    def _focus_project_search(self) -> None:
        if self._active_tab != _PROJECT_TAB:
            return
        try:
            self.query_one("#project-search", Input).focus()
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
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        title = next(
            (p.title for p in (self._projects or []) if p.key == project_key), None
        )
        self.app.push_screen(ProjectHomeScreen(project_key, title=title))

    @on(Button.Pressed, "#project-create-btn")
    def _on_create_button(self) -> None:
        self._open_create_modal()

    def _open_create_modal(self) -> None:
        from tissue.screens.home.create_project_modal import CreateProjectModal

        self.app.push_screen(CreateProjectModal(), self._on_project_created)

    def _on_project_created(self, created_key: str | None) -> None:
        if created_key:
            self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Input.Submitted, "#project-search")
    def _on_project_search(self, event: Input.Submitted) -> None:
        self._project_query = event.value.strip()
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Checkbox.Changed, "#project-archived-toggle")
    def _on_archived_toggle(self, event: Checkbox.Changed) -> None:
        self._include_archived = event.value
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Button.Pressed, "#project-pin-btn")
    def _on_pin_button(self) -> None:
        project = self._selected_project
        if project is not None and project.key:
            self._toggle_pin(project.key)

    @on(Button.Pressed, "#project-archive-btn")
    def _on_archive_button(self) -> None:
        project = self._selected_project
        if project is None or not project.key:
            return
        key = project.key
        if project.archived:  # unarchive only reveals it again → no confirm needed
            self.run_worker(
                self._toggle_archive(key, False),
                exclusive=True,
                group="project-archive",
            )
            return

        from tissue.screens.confirm_modal import ConfirmModal

        def _after(confirmed: bool | None) -> None:
            if confirmed:
                self.run_worker(
                    self._toggle_archive(key, True),
                    exclusive=True,
                    group="project-archive",
                )

        self.app.push_screen(
            ConfirmModal(
                title="Archive project",
                message=(
                    f"Archive {key}? It will be hidden from the list until you "
                    "turn on the “Archived” filter."
                ),
                confirm_label="Archive",
            ),
            _after,
        )

    # ---- project pin (client-side) / archive ---------------------------

    def _pinned_keys(self) -> set[str]:
        server = self.app.config.state.current_server_url or ""
        return set(self.app.config.pinned_project_keys(server))

    def _is_pinned(self, key: str | None) -> bool:
        return bool(key) and key in self._pinned_keys()

    def _sort_projects(self) -> None:
        """Float pinned projects to the top, preserving server order otherwise."""
        if not self._projects:
            return
        pinned = self._pinned_keys()
        self._projects.sort(key=lambda p: (p.key or "") not in pinned)

    def _toggle_pin(self, key: str) -> None:
        server = self.app.config.state.current_server_url or ""
        self.app.config.toggle_pinned_project(server, key)
        self._sort_projects()
        self.refresh(recompose=True)

    async def _toggle_archive(self, key: str, archive: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if archive:
                await client.projects.archive_project(key)
            else:
                await client.projects.unarchive_project(key)
        except TissueApiError as e:
            log.warning("Failed to set archived=%s for %s: %s", archive, key, e)
            self.app.notify("Couldn't change the archive state.", severity="error")
            return
        if archive:
            self.app.notify(
                "Project archived — hidden until you enable the “Archived” filter."
            )
        else:
            self.app.notify("Project unarchived.")
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        labels = {
            "public": "Public",
            "private": "Private",
        }
        label = labels.get(visibility.lower())
        if label is None:  # unknown enum → readable fallback
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
            self._set_wiki_tree_error("Cannot reach server. Press r to retry.")
            return
        except ServerError:
            self._set_wiki_tree_error("Server error. Press r to retry.")
            return
        except TissueApiError as e:
            log.warning("Failed to load wiki tree: %s", e)
            self._set_wiki_tree_error("Failed to load the wiki. Press r to retry.")
            return
        finally:
            self._wiki_loading = False

        self._wiki_error = None
        self._wiki_nodes = list(nodes or [])
        # Full recompose rebuilds the tree from cache; rare (lazy load + manual
        # refresh only), so the lost expand state is acceptable.
        self.refresh(recompose=True)

    def _set_wiki_tree_error(self, message: str) -> None:
        # Toast (seen even with the tree hidden) + recompose. Don't override the
        # user's ctrl+b choice; the content pane surfaces it when the sidebar is
        # hidden (see _wiki_content_widgets).
        self._wiki_error = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    async def _load_wiki_doc(self, wiki_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        if self._wiki_view not in ("doc", "search"):
            # Spinner only when nothing readable is shown — avoids a flash when
            # swapping between already-rendered docs/results on a fast server.
            await self._render_wiki_loading()
        try:
            doc = await client.wiki.get_document(wiki_id)
        except TissueApiError as e:
            log.warning("Failed to load wiki document %s: %s", wiki_id, e)
            self._wiki_view = "error"
            self._wiki_content_error = "Failed to load the document."
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
        self._sync_wiki_tree_cursor(wiki_id)

    def _sync_wiki_tree_cursor(self, wiki_id: int) -> None:
        """Move the tree cursor to the shown document (best-effort), keeping the
        sidebar in sync after in-place navigation (parent link / search result).
        select_node re-emits NodeSelected, but _on_wiki_node_selected ignores it
        because the doc is already loaded."""
        if not self._wiki_sidebar_visible:
            return
        try:
            tree = self.query_one("#wiki-tree", Tree)
        except NoMatches:
            return
        node = self._find_wiki_tree_node(tree.root, wiki_id)
        if node is None:
            return
        ancestor = node.parent
        while ancestor is not None:  # reveal the node if nested under collapsed
            ancestor.expand()
            ancestor = ancestor.parent

        # Expanding queues a tree relayout; defer the move so the node has a
        # computed line (otherwise the cursor won't move to a freshly-revealed
        # nested node, e.g. when opening a child doc from search).
        # Use move_cursor (not select_node): select_node posts NodeSelected,
        # which Tree.auto_expand turns into a _toggle_node — that would collapse
        # the node a click just expanded.
        def _move() -> None:
            tree.move_cursor(node)
            tree.scroll_to_node(node)

        self.call_after_refresh(_move)

    @staticmethod
    def _find_wiki_tree_node(root: TreeNode[int], wiki_id: int) -> TreeNode[int] | None:
        stack = list(root.children)
        while stack:
            node = stack.pop()
            if node.data == wiki_id:
                return node
            stack.extend(node.children)
        return None

    async def _load_wiki_snapshot(self, snapshot_id: int) -> None:
        client = self.app.client
        doc = self._wiki_doc
        if client is None or doc is None or doc.id is None:
            return
        try:
            snap = await client.wiki.get_version(doc.id, snapshot_id)
        except TissueApiError as e:
            log.warning("Failed to load wiki snapshot %s: %s", snapshot_id, e)
            self.app.notify("Failed to load that version.", severity="error")
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
            self.app.notify("Failed to change the lock.", severity="error")
            return
        self.app.notify("Document locked." if lock else "Document unlocked.")
        await self._load_wiki_doc(wiki_id)

    async def _do_wiki_search(self, query: str) -> None:
        client = self.app.client
        if client is None:
            return
        if self._wiki_view not in ("doc", "search"):
            await self._render_wiki_loading()
        try:
            page = await client.wiki.search(keyword=query)
        except TissueApiError as e:
            log.warning("Wiki search failed: %s", e)
            self._wiki_view = "error"
            self._wiki_content_error = "Search failed. Try again."
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
            inner = self.query_one("#wiki-content-inner", Container)
        except NoMatches:
            return
        content.border_title = self._wiki_content_title()
        # Smooth doc→doc navigation: when a document is already shown, swap the
        # content in place instead of tearing the whole pane down. Rebuilding
        # remounts the heavy Markdown body, which flashes blank while it
        # re-parses; reusing it keeps the old text until the new parse is ready.
        if (
            self._wiki_view == "doc"
            and self._wiki_doc is not None
            and self._wiki_snapshot is None
            and inner.query(".wiki-doc-header")
        ):
            await self._update_wiki_doc_in_place(inner)
            return
        await inner.remove_children()
        await inner.mount_all(list(self._wiki_content_widgets()))

    async def _update_wiki_doc_in_place(self, inner: Container) -> None:
        """Update a shown document without rebuilding the pane (see caller)."""
        doc = self._wiki_doc
        if doc is None:
            return
        # Header is small; replace it inside a batch so it doesn't flash.
        with self.app.batch_update():
            await inner.query(".wiki-doc-header").remove()
            await inner.mount(
                *self._wiki_doc_header(doc),
                before=inner.query_one(".wiki-divider"),
            )
        # Body: reuse the Markdown widget via update() (keeps old text visible
        # until the new parse completes). Drop any leftover version banner.
        body = inner.query_one("#wiki-doc-body", Container)
        for banner in body.query(".wiki-version-banner"):
            await banner.remove()
        try:
            md = body.query_one(Markdown)
        except NoMatches:
            await body.mount(Markdown(doc.content or ""))
        else:
            await md.update(doc.content or "")

    async def _render_wiki_doc_body(self) -> None:
        """Swap the body in place (current ↔ snapshot), reusing the Markdown
        widget via update() so switching versions doesn't flash a blank body.
        Only the read-only banner is added/removed."""
        try:
            body = self.query_one("#wiki-doc-body", Container)
        except NoMatches:
            return
        snap = self._wiki_snapshot
        banners = body.query(".wiki-version-banner")
        if snap is not None:
            banner_text = self._snapshot_banner_content(snap)
            if banners:
                banners.first(Static).update(banner_text)
            else:
                await body.mount(
                    Static(banner_text, classes="wiki-version-banner"),
                    before=0,
                )
            content = snap.snapshot_content or ""
        else:
            await banners.remove()
            content = (self._wiki_doc.content if self._wiki_doc else "") or ""
        try:
            md = body.query_one(Markdown)
        except NoMatches:
            await body.mount(Markdown(content))
        else:
            await md.update(content)

    async def _render_wiki_loading(self) -> None:
        """Show a spinner in the content pane while a doc/search fetch runs, so
        the wiki tab gives the same loading feedback the project tab does."""
        try:
            content = self.query_one("#wiki-content", VerticalScroll)
            inner = self.query_one("#wiki-content-inner", Container)
        except NoMatches:
            return
        content.border_title = self._wiki_content_title()
        await inner.remove_children()
        await inner.mount(LoadingIndicator())

    # ---- wiki tab: events ----------------------------------------------

    @on(Tree.NodeSelected)
    def _on_wiki_node_selected(self, event: Tree.NodeSelected) -> None:
        data = event.node.data
        if not isinstance(data, int):
            return
        if self._wiki_doc is not None and self._wiki_doc.id == data:
            # Already showing this doc (e.g. the tree cursor we moved ourselves
            # after a parent-link / in-place navigation) — don't reload.
            return
        if self._wiki_view in ("create", "edit"):
            self.app.notify(
                "Save or cancel your current edit first.", severity="warning"
            )
            return
        self.run_worker(
            self._load_wiki_doc(data), exclusive=True, group="wiki-doc-load"
        )

    def action_open_wiki_parent(self) -> None:
        """Navigate in place to the current document's parent (Parent link)."""
        doc = self._wiki_doc
        if doc is None or doc.parent_document_id is None:
            return
        if self._wiki_view in ("create", "edit"):
            self.app.notify(
                "Save or cancel your current edit first.", severity="warning"
            )
            return
        self.run_worker(
            self._load_wiki_doc(doc.parent_document_id),
            exclusive=True,
            group="wiki-doc-load",
        )

    @on(Input.Submitted, "#wiki-search")
    async def _on_wiki_search_submitted(self, event: Input.Submitted) -> None:
        if self._wiki_view in ("create", "edit"):
            self.app.notify(
                "Save or cancel your current edit first.", severity="warning"
            )
            return
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
        # Block while an editor is open so a half-written draft isn't discarded.
        if self._wiki_view in ("create", "edit"):
            self.app.notify(
                "Save or cancel your current edit first.", severity="warning"
            )
            return
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
        self.refresh_bindings()  # hide 'r' while the editor is open
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
        self.refresh_bindings()  # hide 'r' while the editor is open
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
        self.refresh_bindings()  # restore 'r' now that the editor closed
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
                self.app.notify(f"Document '{event.title}' created.")
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
                self.app.notify("Document updated.")
        except TissueApiError as e:
            log.warning("Wiki save failed: %s", e)
            self._wiki_saving = False
            reason = self._wiki_failure_reason(e)
            self.app.notify(
                f"Failed to create document: {reason}"
                if creating
                else f"Failed to update document: {reason}",
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
        self.refresh_bindings()  # editor closed → 'r' is meaningful again

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
                    "Document created, but couldn't move the current page under it.",
                    severity="warning",
                )
        return new_id

    @staticmethod
    def _wiki_failure_reason(exc: TissueApiError) -> str:
        return exc.detail or exc.title or str(exc)
