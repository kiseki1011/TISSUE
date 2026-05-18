import logging
from datetime import datetime

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import (
    Button,
    DataTable,
    Footer,
    Header,
    Label,
    Static,
    TabbedContent,
    TabPane,
)

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.invitation_detail import InvitationDetail
from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen
from tissue.screens.workspace_create import WorkspaceCreateModal
from tissue.screens.workspace_home import WorkspaceHomeScreen
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView
from tissue.widgets.text_button import TextButton

log = logging.getLogger(__name__)


class HomeScreen(TissueScreen):
    """The post-login screen.

    This screen contains the following TabPanes:
        - Workspaces | Invitations | My Account
    """

    CSS_PATH = "home.tcss"

    BINDINGS = [
        Binding("c", "create_workspace", "create workspace"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self._selected_invitation: InvitationDetail | None = None

    def compose(self) -> ComposeResult:
        yield Header()
        with TabbedContent(initial="workspaces-tab", id="home-tabs"):
            with TabPane(i18n.get("home_tab_workspaces"), id="workspaces-tab"):
                with Horizontal(id="ws-toolbar"):
                    with Horizontal(id="ws-toolbar-table-area"):
                        yield TextButton(
                            i18n.get("home_workspace_create_btn"),
                            id="ws_create_btn",
                        )
                    yield Container(id="ws-toolbar-detail-area")
                yield TableDetailSplitView[WorkspaceSummaryResponse](
                    id="workspaces-split",
                    columns=self._workspace_columns(),
                    row_builder=self._workspace_row,
                    detail_renderer=self._render_workspace_detail,
                )
            with TabPane(i18n.get("home_tab_invitations"), id="invitations-tab"):
                yield TableDetailSplitView[InvitationDetail](
                    id="invitations-split",
                    columns=self._invitation_columns(),
                    row_builder=self._invitation_row,
                    detail_renderer=self._render_invitation_detail,
                )
            with TabPane(i18n.get("home_tab_account"), id="account-tab"):
                yield Static(
                    i18n.get("home_account_placeholder"),
                    classes="placeholder",
                )
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

        self.query_one("#workspaces-split", TableDetailSplitView).populate(
            self._workspaces()
        )
        self.query_one("#invitations-split", TableDetailSplitView).populate(
            self._invitations()
        )

    def action_create_workspace(self) -> None:
        self.app.push_screen(WorkspaceCreateModal(), self._on_workspace_created)

    @on(Button.Pressed, "#ws_create_btn")
    def on_create_btn_pressed(self) -> None:
        self.action_create_workspace()

    @on(DataTable.RowSelected)
    def on_data_table_row_selected(self, event: DataTable.RowSelected) -> None:
        """Enter on a row inside the workspaces table opens to that workspace home.
        Identifies which split view the event came from using `query_ancestor`.
        """
        event_owner = event.data_table.query_ancestor(TableDetailSplitView)
        if event_owner is None or event_owner.id != "workspaces-split":
            return
        workspaces = self._workspaces()
        if 0 <= event.cursor_row < len(workspaces):
            self.app.push_screen(WorkspaceHomeScreen(workspaces[event.cursor_row]))

    def _on_workspace_created(self, response: WorkspaceCreateResponse | None) -> None:
        """Called when the create modal closes.

        None → user cancelled.
        Otherwise the workspace was created and cached_workspaces is already
        refreshed (`client.create_workspace` fetches the list internally).
        Refresh the table and switch to the new workspace's home screen.
        """
        if response is None or response.workspace_key is None:
            return

        self.query_one("#workspaces-split", TableDetailSplitView).populate(
            self._workspaces()
        )

        created = next(
            (
                ws
                for ws in self._workspaces()
                if ws.workspace_key == response.workspace_key
            ),
            None,
        )
        if created is not None:
            self.app.push_screen(WorkspaceHomeScreen(created))

    def _workspace_columns(self) -> list[Column]:
        return [
            Column("no", i18n.get("home_col_no"), 2),
            Column("key", i18n.get("home_col_workspace_key"), 20),
            Column("name", i18n.get("home_col_name"), 24),
            Column("status", i18n.get("home_col_status"), 14),
            Column("created", i18n.get("home_col_created"), 18),
        ]

    def _workspace_row(self, idx: int, ws: WorkspaceSummaryResponse) -> list[str]:
        return [
            str(idx),
            ws.workspace_key or "-",
            ws.name or "-",
            "-",  # TODO: status placeholder (soft-deleted / archived)
            _fmt_dt(ws.created_at),
        ]

    def _render_workspace_detail(
        self, ws: WorkspaceSummaryResponse | None, container: Container
    ) -> None:
        container.remove_children()
        if ws is None:
            container.mount(
                Static(i18n.get("home_workspace_empty"), classes="detail-empty")
            )
            return
        rows = [
            (i18n.get("home_workspace_key"), ws.workspace_key or "-"),
            (i18n.get("home_workspace_name"), ws.name or "-"),
            (i18n.get("home_workspace_description"), ws.description or "-"),
            (
                i18n.get("home_workspace_remind"),
                i18n.get("home_workspace_remind_placeholder"),
            ),
            (i18n.get("home_workspace_role"), ws.my_role or "-"),
            (i18n.get("home_workspace_status"), "-"),
            (i18n.get("home_workspace_members"), "-"),
            (i18n.get("home_workspace_created"), _fmt_dt(ws.created_at)),
        ]
        for key, value in rows:
            container.mount(_detail_row(key, value))

    def _invitation_columns(self) -> list[Column]:
        return [
            Column("no", i18n.get("home_col_no"), 2),
            Column("workspace_key", i18n.get("home_col_workspace_key"), 20),
            Column("workspace_name", i18n.get("home_col_name"), 24),
            Column("inviter", i18n.get("home_col_inviter"), 18),
            Column("invited_at", i18n.get("home_col_invited_at"), 18),
        ]

    def _invitation_row(self, idx: int, inv: InvitationDetail) -> list[str]:
        inviter = inv.inviter_name or inv.inviter_email or "-"
        return [
            str(idx),
            inv.workspace_key or "-",
            inv.workspace_name or "-",
            inviter,
            _fmt_dt(inv.invited_at),
        ]

    def _render_invitation_detail(
        self, inv: InvitationDetail | None, container: Container
    ) -> None:
        container.remove_children()
        self._selected_invitation = inv
        if inv is None:
            container.mount(
                Static(i18n.get("home_invitation_empty"), classes="detail-empty")
            )
            return
        inviter = inv.inviter_name or inv.inviter_email or "-"
        rows = [
            (i18n.get("home_invitation_inviter"), inviter),
            (i18n.get("home_col_workspace_key"), inv.workspace_key or "-"),
            (i18n.get("home_col_name"), inv.workspace_name or "-"),
            (i18n.get("home_invitation_role"), inv.workspace_role or "-"),
            (i18n.get("home_invitation_invited_at"), _fmt_dt(inv.invited_at)),
        ]
        for key, value in rows:
            container.mount(_detail_row(key, value))
        container.mount(
            Horizontal(
                Button(
                    i18n.get("home_invitation_accept_btn"),
                    id="inv_accept_btn",
                    variant="success",
                ),
                Button(
                    i18n.get("home_invitation_reject_btn"),
                    id="inv_reject_btn",
                    variant="error",
                ),
                classes="invitation-actions",
            )
        )

    def _workspaces(self) -> list[WorkspaceSummaryResponse]:
        client = self.app.client
        return list(client.cached_workspaces or []) if client is not None else []

    def _invitations(self) -> list[InvitationDetail]:
        client = self.app.client
        return list(client.cached_invitations or []) if client is not None else []

    @on(Button.Pressed, "#inv_accept_btn")
    async def on_invitation_accept_pressed(self) -> None:
        inv = self._selected_invitation
        client = self.app.client
        if inv is None or inv.invitation_id is None or client is None:
            return
        try:
            await client.accept_invitation(inv.invitation_id)
        except TissueApiError as e:
            self.app.notify(
                i18n.get("invitation_accept_failed", reason=str(e)),
                severity="error",
            )
            return
        self.app.notify(i18n.get("invitation_accept_success"))
        self._refresh_invitations_view()
        # refresh the workspaces cache
        self.query_one("#workspaces-split", TableDetailSplitView).populate(
            self._workspaces()
        )

    @on(Button.Pressed, "#inv_reject_btn")
    async def on_invitation_reject_pressed(self) -> None:
        inv = self._selected_invitation
        client = self.app.client
        if inv is None or inv.invitation_id is None or client is None:
            return
        try:
            await client.reject_invitation(inv.invitation_id)
        except TissueApiError as e:
            self.app.notify(
                i18n.get("invitation_reject_failed", reason=str(e)),
                severity="error",
            )
            return
        self.app.notify(i18n.get("invitation_reject_success"))
        self._refresh_invitations_view()

    def _refresh_invitations_view(self) -> None:
        self.query_one("#invitations-split", TableDetailSplitView).populate(
            self._invitations()
        )


def _detail_row(key: str, value: str) -> Horizontal:
    return Horizontal(
        Label(f"{key}:", classes="detail-key"),
        Label(value, classes="detail-value"),
        classes="detail-row",
    )


def _fmt_dt(dt: datetime | None) -> str:
    if dt is None:
        return "-"
    return dt.astimezone().strftime("%Y-%m-%d %H:%M")
