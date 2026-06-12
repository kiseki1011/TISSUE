import logging

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Vertical
from textual.css.query import NoMatches
from textual.widgets import Button, DataTable, Footer, LoadingIndicator, Static

from tissue.api.errors import ConnectionFailed, ServerError, TissueApiError
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.i18n.manager import i18n
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView

log = logging.getLogger(__name__)


class ProjectListScreen(RefreshableScreen):
    """Post-login landing: the project picker.

    Shows a list + detail split view of the projects the member can see.
    Selecting a row opens its ProjectHome; the detail pane carries a
    "create new project" action. When there are no projects, a prompt offers
    to create the first one.

    State lives on the instance (`_projects` / `_error_message`) so a language
    recompose re-renders from cached data without another round trip.
    """

    DEFAULT_CSS = """
    ProjectListScreen #screen-body {
        padding: 0;
    }
    ProjectListScreen .status-center {
        width: 100%;
        height: 100%;
        content-align: center middle;
        text-align: center;
        color: $text-muted;
    }
    ProjectListScreen .detail-body {
        width: 100%;
        height: auto;
    }
    ProjectListScreen .detail-title {
        width: 100%;
        text-style: bold;
        color: $text;
        padding-bottom: 1;
    }
    ProjectListScreen .detail-row {
        width: 100%;
        height: auto;
    }
    ProjectListScreen .detail-key {
        width: 14;
        color: $text-muted;
    }
    ProjectListScreen .detail-value {
        width: 1fr;
        color: $text;
    }
    ProjectListScreen .detail-desc {
        width: 100%;
        padding-top: 1;
        color: $text;
    }
    ProjectListScreen .detail-hint {
        width: 100%;
        padding-top: 1;
        color: $text-muted;
        text-style: italic;
    }
    ProjectListScreen .detail-empty {
        width: 100%;
        color: $text-muted;
    }
    """

    def __init__(self) -> None:
        super().__init__()
        self._projects: list[ProjectSummary] | None = None
        self._error_message: str | None = None
        self._loading = False
        self._empty_prompt_shown = False

    def compose(self) -> ComposeResult:
        with Container(id="screen-body"):
            if self._error_message is not None:
                yield Static(self._error_message, classes="status-center")
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
                self.call_after_refresh(self._focus_table)
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load(), exclusive=True, group="project-load")

    async def refresh_data(self) -> None:
        await self._load()

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            log.error("ProjectList load attempted but TissueClient is not set")
            return
        if self._loading:
            return
        self._loading = True
        try:
            page = await client.projects.list_projects(size=100)
        except ConnectionFailed:
            self._set_error(i18n.get("project_list_error_unreachable"))
            return
        except ServerError:
            self._set_error(i18n.get("project_list_error_server"))
            return
        except TissueApiError as e:
            log.warning("Failed to load projects: %s", e)
            self._set_error(i18n.get("project_list_error_generic"))
            return
        finally:
            self._loading = False

        self._error_message = None
        self._projects = list(page.content or [])
        self.refresh(recompose=True)
        if not self._projects:
            self.call_after_refresh(self._prompt_create_if_empty)

    def _set_error(self, message: str) -> None:
        self._error_message = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    # ---- table / detail rendering ---------------------------------------

    def _row(self, idx: int, project: ProjectSummary) -> list[str | Text]:
        # Wrap the free-text title in Text so a title containing Rich-markup
        # brackets (e.g. "[urgent] fix") renders verbatim instead of being
        # parsed as console markup. key/visibility/date are markup-safe.
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
        try:
            self.query_one("#project-split").query_one(DataTable).focus()
        except NoMatches:
            pass

    # ---- navigation / actions -------------------------------------------

    @on(DataTable.RowSelected)
    def _on_row_selected(self, event: DataTable.RowSelected) -> None:
        idx = event.cursor_row
        if self._projects and 0 <= idx < len(self._projects):
            self._open_project(self._projects[idx].key)

    def _open_project(self, project_key: str | None) -> None:
        if not project_key:
            return
        # ProjectHome lands next turn; selection is wired and ready.
        self.app.notify(i18n.get("project_open_pending", key=project_key), timeout=3)

    @on(Button.Pressed, "#project-create-btn")
    def _on_create_button(self) -> None:
        self._open_create_modal()

    # ---- empty-state prompt / create flow -------------------------------

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
            self.run_worker(self._load(), exclusive=True, group="project-load")

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        key = f"project_visibility_{visibility.lower()}"
        label = i18n.get(key)
        if label == key:  # unknown enum → readable fallback
            return visibility.replace("_", " ").title()
        return label
