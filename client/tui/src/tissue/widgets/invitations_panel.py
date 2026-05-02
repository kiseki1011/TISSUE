import logging
from pathlib import Path

from textual import on, work
from textual.app import ComposeResult
from textual.containers import Container, Horizontal, Vertical
from textual.widgets import Button, Label, ListItem, ListView

from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.invitation import InvitationAPI
from tissue.i18n.manager import i18n
from tissue.models.invitation import InvitationSummary
from tissue.widgets.i18n_widgets import I18nButton, I18nLabel
from tissue.widgets.workspaces_panel import WorkspacesPanel

log = logging.getLogger(__name__)

_CSS_PATH = Path(__file__).parent / "css" / "invitations_panel.tcss"

ACCEPT_PREFIX = "accept-"
REJECT_PREFIX = "reject-"


class _InvitationListItem(ListItem):
    def __init__(self, summary: InvitationSummary) -> None:
        project_text = (
            ", ".join(summary.project_keys)
            if summary.project_keys
            else i18n.get("invitation_workspace_only")
        )
        super().__init__(
            Vertical(
                Horizontal(
                    Label(
                        f"{summary.workspace_name} ({summary.workspace_key})",
                        classes="inv-ws",
                    ),
                    I18nButton(
                        key="accept_btn",
                        id=f"{ACCEPT_PREFIX}{summary.invitation_id}",
                        classes="-success",
                    ),
                    I18nButton(
                        key="reject_btn",
                        id=f"{REJECT_PREFIX}{summary.invitation_id}",
                        classes="-secondary",
                    ),
                    classes="inv-row",
                ),
                Label(project_text, classes="inv-projects"),
                I18nLabel(
                    "invitation_inviter",
                    fmt_args={"name": summary.inviter_name},
                    classes="inv-inviter",
                ),
            )
        )
        self.summary = summary


class InvitationsPanel(Container):
    DEFAULT_CSS = _CSS_PATH.read_text()

    def compose(self) -> ComposeResult:
        with Vertical():
            yield I18nLabel("invitations_heading", classes="panel-heading")
            yield I18nLabel(
                "invitations_empty",
                id="empty_state",
                classes="empty-state",
            )
            yield ListView(id="inv_list")

    def on_mount(self) -> None:
        self.refresh_data()

    @work(exclusive=True, group="inv_list")
    async def refresh_data(self) -> None:
        try:
            items = await InvitationAPI(self.app.client).list_my()
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Invitation list failed: %s", e)
            self.app.notify(
                i18n.get("invitation_action_failed"),
                severity="error",
            )
            return
        pending = [it for it in items if it.status == "PENDING"]
        self._apply_items(pending)

    def _apply_items(self, items: list[InvitationSummary]) -> None:
        list_view = self.query_one("#inv_list", ListView)
        empty = self.query_one("#empty_state", I18nLabel)
        list_view.clear()
        if not items:
            list_view.display = False
            empty.display = True
            return
        empty.display = False
        list_view.display = True
        for item in items:
            list_view.append(_InvitationListItem(item))

    @on(Button.Pressed)
    def _on_button_pressed(self, event: Button.Pressed) -> None:
        button_id = event.button.id or ""
        if button_id.startswith(ACCEPT_PREFIX):
            try:
                inv_id = int(button_id[len(ACCEPT_PREFIX) :])
            except ValueError:
                return
            event.stop()
            self._do_action(inv_id, accept=True)
        elif button_id.startswith(REJECT_PREFIX):
            try:
                inv_id = int(button_id[len(REJECT_PREFIX) :])
            except ValueError:
                return
            event.stop()
            self._do_action(inv_id, accept=False)

    @work(exclusive=True, group="inv_action")
    async def _do_action(self, invitation_id: int, *, accept: bool) -> None:
        api = InvitationAPI(self.app.client)
        try:
            if accept:
                await api.accept(invitation_id)
            else:
                await api.reject(invitation_id)
        except (ApiResponseError, ApiNetworkError, TissueApiError) as e:
            log.warning("Invitation action failed: %s", e)
            self.app.notify(
                i18n.get("invitation_action_failed"), severity="error", timeout=3
            )
            return
        msg_key = "invitation_accepted" if accept else "invitation_rejected"
        self.app.notify(i18n.get(msg_key), timeout=2)
        self._remove_row(invitation_id)
        if accept:
            try:
                self.screen.query_one(WorkspacesPanel).refresh_data()
            except Exception as e:
                log.debug("Could not refresh workspaces panel: %s", e)

    def _remove_row(self, invitation_id: int) -> None:
        list_view = self.query_one("#inv_list", ListView)
        for child in list(list_view.children):
            if (
                isinstance(child, _InvitationListItem)
                and child.summary.invitation_id == invitation_id
            ):
                child.remove()
                break
        if not list(list_view.children):
            list_view.display = False
            self.query_one("#empty_state", I18nLabel).display = True
