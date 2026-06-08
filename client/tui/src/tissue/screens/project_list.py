import logging

from textual.app import ComposeResult
from textual.containers import Container
from textual.widgets import Footer, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import PostAuthScreen

log = logging.getLogger(__name__)


class ProjectListScreen(PostAuthScreen):
    """Post-login landing: the project list / dashboard.

    Placeholder for now — the real project picker / my-work view lands later.
    """

    DEFAULT_CSS = """
    ProjectListScreen #screen-body {
        align: center middle;
    }
    ProjectListScreen #project-placeholder {
        width: auto;
        height: auto;
        color: $text-muted;
        text-align: center;
    }
    """

    def compose(self) -> ComposeResult:
        yield Container(
            Static(i18n.get("project_list_placeholder"), id="project-placeholder"),
            id="screen-body",
        )
        yield Footer()
