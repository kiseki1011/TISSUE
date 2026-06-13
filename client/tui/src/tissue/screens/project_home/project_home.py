import logging

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Footer, Static

from tissue.api.errors import ConnectionFailed, ServerError, TissueApiError
from tissue.api.generated.models.project_detail import ProjectDetail
from tissue.i18n.manager import i18n
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row

log = logging.getLogger(__name__)


class ProjectHomeScreen(RefreshableScreen):
    """Per-project hub reached from the project picker.

    Shows the project summary (title, key, visibility, timestamps, description)
    and a menu of sections (Issues / Sprints / Wiki / Members) that each open a
    dedicated screen. The hub is usable the moment it mounts: the header title
    and the section menu render from constructor args, while the descriptive
    metadata is filled in by a detail fetch in the background.

    State lives on the instance (`_detail` / `_error_message`) so a language
    recompose re-renders from cached data without another round trip.
    """

    # (section id, label key, description key). The id maps 1:1 to a button id
    # and to the i18n label key, so a single press handler can resolve both.
    _SECTIONS = (
        ("issues", "project_home_nav_issues", "project_home_nav_issues_desc"),
        ("sprints", "project_home_nav_sprints", "project_home_nav_sprints_desc"),
        ("wiki", "project_home_nav_wiki", "project_home_nav_wiki_desc"),
        ("members", "project_home_nav_members", "project_home_nav_members_desc"),
    )

    DEFAULT_CSS = """
    ProjectHomeScreen #screen-body {
        padding: 1 2;
    }
    ProjectHomeScreen #project-home {
        width: 100%;
        height: 1fr;
    }
    ProjectHomeScreen .home-header {
        width: 100%;
        height: auto;
        padding: 1 2;
        margin-bottom: 1;
    }
    ProjectHomeScreen .home-title {
        width: 100%;
        text-style: bold;
        color: $text;
        padding-bottom: 1;
    }
    ProjectHomeScreen .detail-row {
        width: 100%;
        height: auto;
    }
    ProjectHomeScreen .detail-key {
        width: 14;
        color: $text-muted;
    }
    ProjectHomeScreen .detail-value {
        width: 1fr;
        color: $text;
    }
    ProjectHomeScreen .home-desc {
        width: 100%;
        padding-top: 1;
        color: $text;
    }
    ProjectHomeScreen .home-meta-loading, ProjectHomeScreen .home-meta-error {
        width: 100%;
        padding-top: 1;
        color: $text-muted;
    }
    ProjectHomeScreen .home-badge {
        width: auto;
        padding: 0 1;
        margin-top: 1;
        background: $warning 30%;
        color: $text;
    }
    ProjectHomeScreen .home-sections {
        width: 100%;
        height: auto;
        padding: 1 2;
    }
    ProjectHomeScreen .home-nav-btn {
        width: 100%;
    }
    ProjectHomeScreen .home-nav-desc {
        width: 100%;
        color: $text-muted;
        padding: 0 0 1 1;
    }
    ProjectHomeScreen .home-back-btn {
        width: auto;
        margin-top: 1;
    }
    """

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title
        self._detail: ProjectDetail | None = None
        self._error_message: str | None = None
        self._loading = False
        self._focused_once = False

    def compose(self) -> ComposeResult:
        with Container(id="screen-body"):
            with VerticalScroll(id="project-home"):
                yield self._build_header()
                yield self._build_sections()
                yield Button(
                    i18n.get("project_home_back"),
                    id="home-back-btn",
                    classes="home-back-btn",
                )
        yield Footer()
        # Focus the first section once, on the initial mount only. Re-focusing on
        # every recompose would race app._refocus_by_id and steal focus on a
        # language change.
        if not self._focused_once:
            self._focused_once = True
            self.call_after_refresh(self._focus_first_nav)

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load(), exclusive=True, group="project-home-load")

    async def refresh_data(self) -> None:
        await self._load()

    # ---- rendering ------------------------------------------------------

    def _build_header(self) -> Vertical:
        detail = self._detail
        title = (detail.title if detail else None) or self._title or self._project_key
        children: list = [
            Static(title, markup=False, classes="home-title"),
            detail_row(i18n.get("project_col_key"), self._project_key),
        ]
        if detail is not None:
            children.append(
                detail_row(
                    i18n.get("project_col_visibility"),
                    self._visibility_label(detail.visibility),
                )
            )
            children.append(
                detail_row(
                    i18n.get("project_field_created"),
                    format_relative(detail.created_at),
                )
            )
            children.append(
                detail_row(
                    i18n.get("project_col_updated"),
                    format_relative(detail.last_updated_at),
                )
            )
            if detail.archived:
                children.append(
                    Static(
                        i18n.get("project_home_archived_badge"), classes="home-badge"
                    )
                )
            children.append(
                Static(
                    detail.description or i18n.get("project_no_description"),
                    markup=False,
                    classes="home-desc",
                )
            )
            if self._error_message is not None:
                # A refresh failed but we still hold prior data: keep showing it
                # and flag the staleness inline rather than wiping the panel.
                children.append(Static(self._error_message, classes="home-meta-error"))
        elif self._error_message is not None:
            children.append(Static(self._error_message, classes="home-meta-error"))
        else:
            children.append(
                Static(i18n.get("project_home_loading"), classes="home-meta-loading")
            )
        return Vertical(*children, classes="panel home-header")

    def _build_sections(self) -> Vertical:
        children: list = []
        for section_id, label_key, desc_key in self._SECTIONS:
            children.append(
                Button(
                    i18n.get(label_key),
                    id=f"home-nav-{section_id}",
                    classes="home-nav-btn",
                )
            )
            children.append(Static(i18n.get(desc_key), classes="home-nav-desc"))
        panel = Vertical(*children, classes="panel home-sections")
        panel.border_title = i18n.get("project_home_sections_label")
        return panel

    def _focus_first_nav(self) -> None:
        try:
            self.query_one("#home-nav-issues", Button).focus()
        except NoMatches:
            pass

    # ---- data -----------------------------------------------------------

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            log.error("ProjectHome load attempted but TissueClient is not set")
            return
        if self._loading:
            return
        self._loading = True
        try:
            detail = await client.projects.get_project_detail(self._project_key)
        except ConnectionFailed:
            self._set_error(i18n.get("project_home_error_unreachable"))
            return
        except ServerError:
            self._set_error(i18n.get("project_home_error_server"))
            return
        except TissueApiError as e:
            log.warning("Failed to load project detail: %s", e)
            self._set_error(i18n.get("project_home_error_generic"))
            return
        finally:
            self._loading = False

        self._error_message = None
        self._detail = detail
        self.refresh(recompose=True)

    def _set_error(self, message: str) -> None:
        self._error_message = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    # ---- navigation -----------------------------------------------------

    @on(Button.Pressed, ".home-nav-btn")
    def _on_nav_pressed(self, event: Button.Pressed) -> None:
        section_id = (event.button.id or "").removeprefix("home-nav-")
        label = i18n.get(f"project_home_nav_{section_id}")
        # Section screens land in later turns; the hub wiring is ready.
        self.app.notify(i18n.get("project_home_nav_pending", section=label), timeout=3)

    @on(Button.Pressed, "#home-back-btn")
    def _on_back_pressed(self) -> None:
        if len(self.app.screen_stack) > 1:
            self.app.pop_screen()
        else:  # defensive: direct-entry (e.g. future last-project recall)
            from tissue.screens.home.home import HomeScreen

            self.app.switch_screen(HomeScreen())

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        key = f"project_visibility_{visibility.lower()}"
        label = i18n.get(key)
        if label == key:  # unknown enum → readable fallback
            return visibility.replace("_", " ").title()
        return label
