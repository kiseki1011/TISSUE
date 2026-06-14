from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Vertical
from textual.widgets import Button, Static

from tissue.screens.base import PostAuthScreen


class ProjectHomeScreen(PostAuthScreen):
    """Placeholder for the per-project hub.

    The full hub (issues / sprints / wiki / members navigation) is deferred to a
    later milestone. For now this only confirms which project was opened and
    offers a way back, so the project picker's navigation stays wired end to end.
    Keeps the `(project_key, title=...)` constructor so the picker is unchanged.
    """

    CSS_PATH = "project_home.tcss"

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title

    def compose_content(self) -> ComposeResult:
        heading = self._title or self._project_key
        with Container(id="screen-body"):
            with Vertical(id="project-home-placeholder"):
                yield Static(heading, markup=False, classes="placeholder-title")
                yield Static(
                    "This project view is coming soon.", classes="placeholder-msg"
                )
                yield Button("← Back to projects", id="home-back-btn")

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

    def top_bar_breadcrumb(self) -> str:
        return f"Projects ▸ {self._title or self._project_key}"

    @on(Button.Pressed, "#home-back-btn")
    def _on_back_pressed(self) -> None:
        if len(self.app.screen_stack) > 1:
            self.app.pop_screen()
        else:
            from tissue.screens.home.home import HomeScreen

            self.app.switch_screen(HomeScreen())
