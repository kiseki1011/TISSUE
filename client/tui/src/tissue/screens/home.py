import logging
from datetime import datetime
from pathlib import Path

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
    Rule,
    Static,
    TabbedContent,
    TabPane,
)

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.invitation_detail import InvitationDetail
from tissue.api.generated.models.member_profile import MemberProfile
from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.rendering.icon import make_icon_widget
from tissue.screens.base import TissueScreen
from tissue.screens.workspace_create import WorkspaceCreateModal
from tissue.screens.workspace_home import WorkspaceHomeScreen
from tissue.widgets.sidebar_nav_button import SidebarNavButton
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView
from tissue.widgets.text_button import TextButton

_PROFILE_ASSET_DIR = Path(__file__).parent.parent / "assets" / "profile"

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

    INVITATION_POLL_INTERVAL_SECONDS = 15

    def __init__(self) -> None:
        super().__init__()
        self._selected_invitation: InvitationDetail | None = None
        self._account_view: str | None = None

    def compose(self) -> ComposeResult:
        yield Header()
        with TabbedContent(initial="workspaces-tab", id="home-tabs"):
            with TabPane(i18n.get("home_tab_workspaces"), id="workspaces-tab"):
                with Horizontal(classes="tab-toolbar"):
                    with Horizontal(classes="tab-toolbar-table-area"):
                        yield TextButton(
                            i18n.get("home_workspace_create_btn"),
                            id="ws_create_btn",
                        )
                    yield Container(classes="tab-toolbar-detail-area")
                yield TableDetailSplitView[WorkspaceSummaryResponse](
                    id="workspaces-split",
                    columns=self._workspace_columns(),
                    row_builder=self._workspace_row,
                    detail_renderer=self._render_workspace_detail,
                    items=self._workspaces(),
                    table_title=i18n.get("home_workspaces_table_title"),
                    detail_title=i18n.get("home_detail_title"),
                )
            with TabPane(i18n.get("home_tab_invitations"), id="invitations-tab"):
                with Horizontal(classes="tab-toolbar"):
                    with Horizontal(classes="tab-toolbar-table-area"):
                        yield TextButton(
                            i18n.get("home_invitation_refresh_btn"),
                            id="inv_refresh_btn",
                        )
                    yield Container(classes="tab-toolbar-detail-area")
                yield TableDetailSplitView[InvitationDetail](
                    id="invitations-split",
                    columns=self._invitation_columns(),
                    row_builder=self._invitation_row,
                    detail_renderer=self._render_invitation_detail,
                    items=self._invitations(),
                    table_title=i18n.get("home_invitations_table_title"),
                    detail_title=i18n.get("home_detail_title"),
                )
            with TabPane(i18n.get("home_tab_account"), id="account-tab"):
                with Horizontal(id="account-split"):
                    yield self._build_account_profile_pane()
                    yield self._build_account_detail_pane()
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.set_interval(
            self.INVITATION_POLL_INTERVAL_SECONDS,
            self._poll_invitations,
        )
        self._show_account_view("edit_profile")

    async def _poll_invitations(self) -> None:
        client = self.app.client
        if client is None:
            return

        before_ids = [inv.invitation_id for inv in self._invitations()]
        try:
            await client.invitations.refresh()
        except TissueApiError as e:
            log.warning("Failed to refresh invitations: %s", e)
            return

        after = self._invitations()
        if [inv.invitation_id for inv in after] == before_ids:
            return

        if not self.is_mounted:
            return
        self.query_one("#invitations-split", TableDetailSplitView).populate(after)

    def action_create_workspace(self) -> None:
        self.app.push_screen(WorkspaceCreateModal(), self._on_workspace_created)

    @on(Button.Pressed, "#ws_create_btn")
    def on_create_btn_pressed(self) -> None:
        self.action_create_workspace()

    @on(Button.Pressed, "#inv_refresh_btn")
    async def on_invitation_refresh_pressed(self) -> None:
        await self._poll_invitations()

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
        Otherwise the workspace was created and the workspaces cache is
        refreshed.
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
            Column("key", i18n.get("home_col_workspace_key"), 16),
            Column("name", i18n.get("home_col_name"), 24),
            Column("status", i18n.get("home_col_status"), 12),
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
        self,
        ws: WorkspaceSummaryResponse | None,
        content: Container,
        actions: Container,
    ) -> None:
        content.remove_children()
        actions.remove_children()
        if ws is None:
            content.mount(
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
            content.mount(_detail_row(key, value))

    def _invitation_columns(self) -> list[Column]:
        return [
            Column("no", i18n.get("home_col_no"), 2),
            Column("workspace_key", i18n.get("home_col_workspace_key"), 16),
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

    # TODO: invited projects도 추가
    def _render_invitation_detail(
        self,
        inv: InvitationDetail | None,
        content: Container,
        actions: Container,
    ) -> None:
        content.remove_children()
        actions.remove_children()
        self._selected_invitation = inv
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
            (i18n.get("home_invitation_invited_at"), _fmt_dt(inv.invited_at)),
        ]
        for key, value in rows:
            content.mount(_detail_row(key, value))
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

    def _workspaces(self) -> list[WorkspaceSummaryResponse]:
        client = self.app.client
        return list(client.workspaces.cached or []) if client is not None else []

    def _invitations(self) -> list[InvitationDetail]:
        client = self.app.client
        return list(client.invitations.cached or []) if client is not None else []

    @on(Button.Pressed, "#inv_accept_btn")
    async def on_invitation_accept_pressed(self) -> None:
        inv = self._selected_invitation
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
            await client.invitations.reject(inv.invitation_id)
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

    def _build_account_profile_pane(self) -> Container:
        profile = self._cached_profile()

        image = make_icon_widget(self._profile_image_path())
        image.add_class("account-profile-image")

        info_rows = [
            _detail_row(
                i18n.get("home_account_label_name"),
                (profile.name if profile and profile.name else "-"),
            ),
            _detail_row(
                i18n.get("home_account_label_username"),
                (profile.username if profile and profile.username else "-"),
            ),
            _detail_row(
                i18n.get("home_account_label_email"),
                (profile.email if profile and profile.email else "-"),
            ),
            _detail_row(
                i18n.get("home_account_label_joined"),
                _fmt_dt(profile.joined_at) if profile else "-",
            ),
        ]

        pane = Container(
            Container(image, classes="account-profile-image-wrap"),
            Container(*info_rows, classes="account-profile-info"),
            Rule(classes="account-divider"),
            Container(
                SidebarNavButton(
                    i18n.get("home_account_btn_edit_profile"),
                    id="acc_edit_profile_btn",
                ),
                SidebarNavButton(
                    i18n.get("home_account_btn_change_password"),
                    id="acc_change_password_btn",
                ),
                SidebarNavButton(
                    i18n.get("home_account_btn_withdraw"),
                    id="acc_withdraw_btn",
                    classes="-danger",
                ),
                classes="account-nav-buttons",
            ),
            classes="panel",
            id="account-profile-pane",
        )
        pane.border_title = i18n.get("home_account_profile_title")
        return pane

    def _build_account_detail_pane(self) -> Container:
        pane = Container(
            Container(
                Static(
                    i18n.get("home_account_detail_placeholder"),
                    classes="detail-empty",
                ),
                classes="account-detail-content",
                id="account-detail-content",
            ),
            classes="panel",
            id="account-detail-pane",
        )
        pane.border_title = i18n.get("home_detail_title")
        return pane

    def _cached_profile(self) -> MemberProfile | None:
        client = self.app.client
        return client.account.cached_profile if client is not None else None

    def _profile_image_path(self) -> Path:
        name = (
            "default_profile_dark.png"
            if self._is_dark_theme()
            else "default_profile_light.png"
        )
        return _PROFILE_ASSET_DIR / name

    def _is_dark_theme(self) -> bool:
        theme = getattr(self.app, "current_theme", None)
        if theme is None:
            return True
        return bool(getattr(theme, "dark", True))

    @on(Button.Pressed, "#acc_edit_profile_btn")
    def on_acc_edit_profile_pressed(self) -> None:
        self._show_account_view("edit_profile")

    @on(Button.Pressed, "#acc_change_password_btn")
    def on_acc_change_password_pressed(self) -> None:
        self._show_account_view("change_password")

    @on(Button.Pressed, "#acc_withdraw_btn")
    def on_acc_withdraw_pressed(self) -> None:
        self._show_account_view("withdraw")

    _ACCOUNT_VIEW_TO_BUTTON_ID = {
        "edit_profile": "acc_edit_profile_btn",
        "change_password": "acc_change_password_btn",
        "withdraw": "acc_withdraw_btn",
    }

    _ACCOUNT_VIEW_TO_TITLE_KEY = {
        "edit_profile": "home_account_btn_edit_profile",
        "change_password": "home_account_btn_change_password",
        "withdraw": "home_account_btn_withdraw",
    }

    def _show_account_view(self, view: str) -> None:
        """Re-render the right pane based on the selected sidebar action."""
        self._account_view = view

        selected_id = self._ACCOUNT_VIEW_TO_BUTTON_ID[view]
        for bid in self._ACCOUNT_VIEW_TO_BUTTON_ID.values():
            btn = self.query_one(f"#{bid}", SidebarNavButton)
            btn.set_class(bid == selected_id, "-selected")

        self.query_one("#account-detail-pane", Container).border_title = i18n.get(
            self._ACCOUNT_VIEW_TO_TITLE_KEY[view]
        )

        content = self.query_one("#account-detail-content", Container)
        content.remove_children()

        if view == "withdraw":
            content.mount(
                Static(
                    i18n.get("home_account_withdraw_warning"),
                    classes="warning",
                )
            )

        content.mount(
            Static(i18n.get("home_account_coming_soon"), classes="detail-empty")
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
