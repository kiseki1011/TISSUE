import logging
from datetime import datetime

from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widgets import Footer, Header, Label, Static, TabbedContent, TabPane

from tissue.api.generated.models.invitation_detail import InvitationDetail
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView

log = logging.getLogger(__name__)


class HomeScreen(TissueScreen):
    """Post-login screen: workspaces | invitations | account tabs"""

    CSS_PATH = "home.tcss"

    def compose(self) -> ComposeResult:
        yield Header()
        with TabbedContent(initial="workspaces-tab", id="home-tabs"):
            with TabPane(i18n.get("home_tab_workspaces"), id="workspaces-tab"):
                yield TableDetailSplitView[WorkspaceSummaryResponse](
                    id="workspaces-md",
                    columns=self._workspace_columns(),
                    row_builder=self._workspace_row,
                    detail_renderer=self._render_workspace_detail,
                )
            with TabPane(i18n.get("home_tab_invitations"), id="invitations-tab"):
                yield TableDetailSplitView[InvitationDetail](
                    id="invitations-md",
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
        self.query_one("#workspaces-md", TableDetailSplitView).populate(
            self._workspaces()
        )
        self.query_one("#invitations-md", TableDetailSplitView).populate(
            self._invitations()
        )

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
            "-",  # status placeholder (soft-deleted / archived — coming)
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
            Column("key", i18n.get("home_col_workspace_key"), 20),
            Column("inviter", i18n.get("home_col_inviter"), 24),
            Column("status", i18n.get("home_col_invitation_status"), 14),
            Column("invited_at", i18n.get("home_col_invited_at"), 18),
        ]

    def _invitation_row(self, idx: int, inv: InvitationDetail) -> list[str]:
        inviter = inv.inviter_name or inv.inviter_email or "-"
        return [
            str(idx),
            inv.workspace_key or "-",
            inviter,
            inv.status or "-",
            _fmt_dt(inv.invited_at),
        ]

    def _render_invitation_detail(
        self, inv: InvitationDetail | None, container: Container
    ) -> None:
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
        return list(client.cached_workspaces or []) if client is not None else []

    def _invitations(self) -> list[InvitationDetail]:
        client = self.app.client
        return list(client.cached_invitations or []) if client is not None else []


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
