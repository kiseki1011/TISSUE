import logging

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widget import Widget
from textual.widgets import Button, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.invitation_detail import InvitationDetail
from tissue.i18n.manager import i18n
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView

log = logging.getLogger(__name__)


class InvitationsTab(Widget):
    """Invitations tab content.

    Polls current invitations every `POLL_INTERVAL_SECONDS`.
    """

    DEFAULT_CSS = """
    InvitationsTab {
        width: 100%;
        height: 100%;
    }

    InvitationsTab .invitation-actions {
        width: 100%;
        height: auto;
        align-horizontal: right;
        padding: 0 0;

        Button {
            margin-left: 1;
        }
    }

    InvitationsTab #inv_accept_btn, InvitationsTab #inv_reject_btn {
        background: transparent;
    }

    InvitationsTab #inv_accept_btn:hover,
    InvitationsTab #inv_reject_btn:hover {
        background: $surface-lighten-1;
    }

    InvitationsTab #inv_accept_btn:focus,
    InvitationsTab #inv_reject_btn:focus {
        background: transparent !important;
    }
    """

    POLL_INTERVAL_SECONDS = 15

    def __init__(self) -> None:
        super().__init__()
        self._selected: InvitationDetail | None = None

    def compose(self) -> ComposeResult:
        yield TableDetailSplitView[InvitationDetail](
            id="invitations-split",
            columns=self._columns(),
            row_builder=self._row,
            detail_renderer=self._render_detail,
            items=self._items(),
            table_title=i18n.get("home_invitations_table_title"),
            detail_title=i18n.get("home_detail_title"),
        )

    def on_mount(self) -> None:
        self.set_interval(self.POLL_INTERVAL_SECONDS, self._poll)

    async def refresh_data(self) -> None:
        await self._poll()

    async def _poll(self) -> None:
        client = self.app.client
        if client is None:
            return

        before_ids = [inv.invitation_id for inv in self._items()]
        try:
            await client.invitations.refresh()
        except TissueApiError as e:
            log.warning("Failed to refresh invitations: %s", e)
            return
        after = self._items()
        # Re-fetch/render invitations only if the set has changed.
        if [inv.invitation_id for inv in after] == before_ids:
            return
        if not self.is_mounted:
            return
        self.query_one("#invitations-split", TableDetailSplitView).populate(after)

    def _columns(self) -> list[Column]:
        return [
            Column("no", i18n.get("home_col_no"), 2),
            Column("workspace_key", i18n.get("home_col_workspace_key"), 14),
            Column("workspace_name", i18n.get("home_col_name"), 22),
            Column("inviter", i18n.get("home_col_inviter"), 14),
            Column("invited_at", i18n.get("home_col_invited_at"), 18),
        ]

    def _row(self, idx: int, inv: InvitationDetail) -> list[str]:
        inviter = inv.inviter_name or inv.inviter_email or "-"
        return [
            str(idx),
            inv.workspace_key or "-",
            inv.workspace_name or "-",
            inviter,
            format_relative(inv.invited_at),
        ]

    def _render_detail(
        self,
        inv: InvitationDetail | None,
        content: Container,
        actions: Container,
    ) -> None:
        content.remove_children()
        actions.remove_children()
        self._selected = inv
        if inv is None:
            content.mount(
                Static(i18n.get("home_invitation_empty"), classes="detail-empty")
            )
            return
        inviter = inv.inviter_name or inv.inviter_email or "-"
        rows = [
            (i18n.get("home_invitation_inviter"), inviter),
            (i18n.get("home_col_workspace_key"), inv.workspace_key),
            (i18n.get("home_col_name"), inv.workspace_name),
            (i18n.get("home_invitation_role"), inv.workspace_role),
            (i18n.get("home_invitation_projects"), inv.project_keys or "-"),
            (i18n.get("home_invitation_invited_at"), format_relative(inv.invited_at)),
        ]
        for key, value in rows:
            content.mount(detail_row(key, value))
        actions.mount(
            Horizontal(
                Button(
                    i18n.get("home_invitation_accept_btn"),
                    id="inv_accept_btn",
                    classes="-btn-success",
                ),
                Button(
                    i18n.get("home_invitation_reject_btn"),
                    id="inv_reject_btn",
                    classes="-btn-error",
                ),
                classes="invitation-actions",
            )
        )

    def _items(self) -> list[InvitationDetail]:
        client = self.app.client
        return list(client.invitations.cached or []) if client is not None else []

    def _refresh_view(self) -> None:
        self.query_one("#invitations-split", TableDetailSplitView).populate(
            self._items()
        )

    @on(Button.Pressed, "#inv_accept_btn")
    async def _on_accept_pressed(self) -> None:
        inv = self._selected
        client = self.app.client
        if inv is None or inv.invitation_id is None or client is None:
            return
        try:
            await client.invitations.accept(inv.invitation_id)
        except TissueApiError as e:
            self.app.notify(
                i18n.get("invitation_accept_failed", reason=str(e)),
                severity="error",
            )
            return
        self.app.notify(i18n.get("invitation_accept_success"))
        self._refresh_view()

        from tissue.widgets.workspaces_tab import WorkspacesTab

        try:
            ws_tab = self.screen.query_one(WorkspacesTab)
        except Exception:
            return
        ws_tab.query_one("#workspaces-split", TableDetailSplitView).populate(
            list(client.workspaces.cached or [])
        )

    @on(Button.Pressed, "#inv_reject_btn")
    async def _on_reject_pressed(self) -> None:
        inv = self._selected
        client = self.app.client
        if inv is None or inv.invitation_id is None or client is None:
            return
        try:
            await client.invitations.reject(inv.invitation_id)
        except TissueApiError as e:
            self.app.notify(
                i18n.get("invitation_reject_failed", reason=str(e)),
                severity="error",
            )
            return
        self.app.notify(i18n.get("invitation_reject_success"))
        self._refresh_view()
