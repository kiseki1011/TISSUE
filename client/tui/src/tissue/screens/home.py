import logging

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.widgets import Footer, TabbedContent, TabPane

from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import RefreshableScreen
from tissue.screens.workspace_create import WorkspaceCreateModal
from tissue.widgets.invitations_tab import InvitationsTab
from tissue.widgets.refreshable import Refreshable
from tissue.widgets.workspaces_tab import WorkspacesTab

log = logging.getLogger(__name__)


class HomeScreen(RefreshableScreen):
    """The post-login screen.

    This screen contains the following TabPanes:
        Workspaces | Invitations

        - Workspaces: User's currently joined workspace list and details
        - Invitations: User's received invitation list and details
    """

    CSS_PATH = "home.tcss"

    BINDINGS = [
        Binding("c", "create_workspace", "create workspace"),
    ]

    def compose(self) -> ComposeResult:
        with Container(id="screen-body"):
            with TabbedContent(initial="workspaces-tab", id="home-tabs"):
                with TabPane(i18n.get("home_tab_workspaces"), id="workspaces-tab"):
                    yield WorkspacesTab()
                with TabPane(i18n.get("home_tab_invitations"), id="invitations-tab"):
                    yield InvitationsTab()
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

    def action_create_workspace(self) -> None:
        self.app.push_screen(WorkspaceCreateModal(), self._on_workspace_created)

    async def refresh_data(self) -> None:
        target = self._active_refresh_target()
        if target is not None:
            await target.refresh_data()

    def can_refresh(self) -> bool:
        return self._active_refresh_target() is not None

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "create_workspace":
            return self._is_active_tab("workspaces-tab")
        return super().check_action(action, parameters)

    def _is_active_tab(self, tab_id: str) -> bool:
        try:
            return self.query_one("#home-tabs", TabbedContent).active == tab_id
        except Exception:
            return False

    def _active_refresh_target(self) -> Refreshable | None:
        """Find the first Refreshable in the currently active TabPane."""
        try:
            active = self.query_one("#home-tabs", TabbedContent).active
            pane = self.query_one(f"#{active}", TabPane)
        except Exception:
            return None
        for widget in pane.walk_children():
            if isinstance(widget, Refreshable):
                return widget
        return None

    @on(TabbedContent.TabActivated)
    def _on_tab_activated(self, event: TabbedContent.TabActivated) -> None:
        """Refresh the footer so tab-scoped bindings update on tab change."""
        self.refresh_bindings()

    def _on_workspace_created(self, response: WorkspaceCreateResponse | None) -> None:
        """Modal callback — delegate to the WorkspacesTab to refresh + navigate."""
        try:
            ws_tab = self.query_one(WorkspacesTab)
        except Exception:
            return
        ws_tab.handle_created(response)
