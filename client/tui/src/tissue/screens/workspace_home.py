from textual.app import ComposeResult
from textual.containers import Center
from textual.widgets import Footer, Header, Label

from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class WorkspaceHomeScreen(TissueScreen):
    """Landing screen for a specific workspace's work area."""

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
        # Remember current workspace to restore it on the next app launch
        if self.workspace.workspace_key:
            self.app.config.update_state(
                current_workspace_key=self.workspace.workspace_key
            )
