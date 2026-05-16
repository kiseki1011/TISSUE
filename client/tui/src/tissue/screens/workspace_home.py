from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Center
from textual.widgets import Footer, Header, Label

from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class WorkspaceHomeScreen(TissueScreen):
    """Placeholder landing screen for a single workspace's work area."""

    BINDINGS = [
        Binding("escape", "back", "back"),
    ]

    def __init__(self, workspace: WorkspaceSummaryResponse) -> None:
        super().__init__()
        self.workspace = workspace

    def compose(self) -> ComposeResult:
        yield Header()
        name = self.workspace.name or self.workspace.workspace_key or "-"
        yield Center(
            Label(
                i18n.get("workspace_home_placeholder", name=name),
                id="workspace-home-label",
            )
        )
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

    def action_back(self) -> None:
        self.app.pop_screen()
