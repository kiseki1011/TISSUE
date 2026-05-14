import logging
from datetime import datetime

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widgets import (
    DataTable,
    Footer,
    Header,
    Label,
    Static,
    TabbedContent,
    TabPane,
)

from tissue.api.generated.models.invitation_detail import InvitationDetail
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen

log = logging.getLogger(__name__)


class HomeScreen(TissueScreen):
    """Post-login screen: workspaces | invitations | account tabs"""

    CSS_PATH = "home.tcss"

    def compose(self) -> ComposeResult:
        yield Header()
        with TabbedContent(initial="workspaces-tab", id="home-tabs"):
            with TabPane(i18n.get("home_tab_workspaces"), id="workspaces-tab"):
                with Horizontal(classes="master-detail"):
                    yield DataTable(
                        id="workspaces-table",
                        classes="master-table panel",
                        cursor_type="row",
                        zebra_stripes=True,
                    )
                    yield Container(id="workspace-detail", classes="detail-pane panel")
            with TabPane(i18n.get("home_tab_invitations"), id="invitations-tab"):
                with Horizontal(classes="master-detail"):
                    yield DataTable(
                        id="invitations-table",
                        classes="master-table panel",
                        cursor_type="row",
                        zebra_stripes=True,
                    )
                    yield Container(id="invitation-detail", classes="detail-pane panel")
            with TabPane(i18n.get("home_tab_account"), id="account-tab"):
                yield Static(
                    i18n.get("home_account_placeholder"),
                    classes="placeholder",
                )
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self._setup_workspaces_table()
        self._setup_invitations_table()

    def _setup_workspaces_table(self) -> None:
        table = self.query_one("#workspaces-table", DataTable)
        table.add_column(i18n.get("home_col_no"), key="no", width=3)
        table.add_column(i18n.get("home_col_workspace_key"), key="key", width=14)
        table.add_column(i18n.get("home_col_name"), key="name", width=16)
        table.add_column(i18n.get("home_col_status"), key="status", width=10)
        table.add_column(i18n.get("home_col_created"), key="created", width=18)
        table.add_column(i18n.get("home_col_description"), key="description", width=30)
        workspaces = self._workspaces()
        # No data → no row cursor; otherwise textual highlights the empty
        # row-0 slot at the header line.
        table.show_cursor = bool(workspaces)
        for i, ws in enumerate(workspaces, start=1):
            table.add_row(
                str(i),
                ws.workspace_key or "-",
                ws.name or "-",
                "-",  # status placeholder (soft-deleted / archived — coming)
                _fmt_dt(ws.created_at),
                ws.description or "-",
            )
        self._render_workspace_detail(workspaces[0] if workspaces else None)

    def _setup_invitations_table(self) -> None:
        table = self.query_one("#invitations-table", DataTable)
        table.add_column(i18n.get("home_col_no"), key="no", width=3)
        table.add_column(i18n.get("home_col_workspace_key"), key="key", width=14)
        table.add_column(
            i18n.get("home_col_workspace_name"), key="workspace_name", width=16
        )
        table.add_column(i18n.get("home_col_inviter"), key="inviter", width=20)
        table.add_column(i18n.get("home_col_invitation_status"), key="status", width=10)
        table.add_column(i18n.get("home_col_invited_at"), key="invited_at", width=18)
        invitations = self._invitations()
        table.show_cursor = bool(invitations)
        for i, inv in enumerate(invitations, start=1):
            inviter = inv.inviter_name or inv.inviter_email or "-"
            table.add_row(
                str(i),
                inv.workspace_key or "-",
                inv.workspace_name or "-",
                inviter,
                inv.status or "-",
                _fmt_dt(inv.invited_at),
            )
        self._render_invitation_detail(invitations[0] if invitations else None)

    @on(DataTable.RowHighlighted, "#workspaces-table")
    def on_workspace_row_highlighted(self, event: DataTable.RowHighlighted) -> None:
        workspaces = self._workspaces()
        if 0 <= event.cursor_row < len(workspaces):
            self._render_workspace_detail(workspaces[event.cursor_row])

    @on(DataTable.RowHighlighted, "#invitations-table")
    def on_invitation_row_highlighted(self, event: DataTable.RowHighlighted) -> None:
        invitations = self._invitations()
        if 0 <= event.cursor_row < len(invitations):
            self._render_invitation_detail(invitations[event.cursor_row])

    def _render_workspace_detail(self, ws: WorkspaceSummaryResponse | None) -> None:
        container = self.query_one("#workspace-detail", Container)
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

    def _render_invitation_detail(self, inv: InvitationDetail | None) -> None:
        container = self.query_one("#invitation-detail", Container)
        container.remove_children()
        if inv is None:
            container.mount(
                Static(i18n.get("home_invitation_empty"), classes="detail-empty")
            )
            return
        inviter = inv.inviter_name or inv.inviter_email or "-"
        rows = [
            (i18n.get("home_invitation_inviter"), inviter),
            (i18n.get("home_invitation_workspace_key"), inv.workspace_key or "-"),
            (i18n.get("home_invitation_role"), "-"),
            (i18n.get("home_invitation_status"), inv.status or "-"),
            (i18n.get("home_invitation_invited_at"), _fmt_dt(inv.invited_at)),
        ]
        for key, value in rows:
            container.mount(_detail_row(key, value))

    def _workspaces(self) -> list[WorkspaceSummaryResponse]:
        client = self.app.client
        return list(client.workspaces or []) if client is not None else []

    def _invitations(self) -> list[InvitationDetail]:
        client = self.app.client
        return list(client.invitations or []) if client is not None else []


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
