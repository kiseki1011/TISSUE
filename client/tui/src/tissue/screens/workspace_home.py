from textual import on
from textual.app import ComposeResult
from textual.containers import Center, Horizontal
from textual.widgets import Button, Footer, Header, Input, Label

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class WorkspaceHomeScreen(TissueScreen):
    """Landing screen for a specific workspace's work area."""

    # TEMP: throwaway invite form for testing the accept/reject flow.
    # Remove once the real workspace UI lands.
    DEFAULT_CSS = """
    WorkspaceHomeScreen #invite-form {
        height: auto;
        margin: 1 2;
        align-horizontal: center;
    }
    WorkspaceHomeScreen #invite-form Input {
        width: 40;
        margin-right: 1;
    }
    """

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
        yield Horizontal(
            Input(placeholder="email to invite", id="invite_email"),
            Button("Invite (TEMP)", id="invite_btn", variant="primary"),
            id="invite-form",
        )
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        # Remember current workspace to restore it on the next app launch
        if self.workspace.workspace_key:
            self.app.config.update_state(
                current_workspace_key=self.workspace.workspace_key
            )

    @on(Button.Pressed, "#invite_btn")
    async def on_invite_pressed(self) -> None:
        email_input = self.query_one("#invite_email", Input)
        email = email_input.value.strip()
        client = self.app.client
        ws_key = self.workspace.workspace_key
        if not email or client is None or ws_key is None:
            return
        try:
            resp = await client.workspaces.invite(ws_key, [email])
        except TissueApiError as e:
            self.app.notify(f"Invite failed: {e}", severity="error")
            return
        invited = ", ".join(resp.invited_emails or []) or "-"
        skipped = ", ".join(resp.skipped_emails or []) or "-"
        self.app.notify(f"Invited: {invited} | Skipped: {skipped}")
        email_input.value = ""
