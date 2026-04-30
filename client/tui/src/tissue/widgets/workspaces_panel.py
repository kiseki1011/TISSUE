import logging
from pathlib import Path

from textual import on, work
from textual.app import ComposeResult
from textual.containers import Container, Horizontal, Vertical
from textual.widgets import Button, Label, ListItem, ListView

from tissue.api.errors import ApiNetworkError, TissueApiError
from tissue.api.workspace import WorkspaceAPI
from tissue.i18n.manager import i18n
from tissue.models.workspace import WorkspaceSummary
from tissue.screens.create_workspace import CreateWorkspaceModal
from tissue.widgets.i18n_widgets import I18nButton, I18nLabel

log = logging.getLogger(__name__)

_CSS_PATH = Path(__file__).parent / "css" / "workspaces_panel.tcss"


class _WorkspaceListItem(ListItem):
    def __init__(self, summary: WorkspaceSummary) -> None:
        super().__init__(
            Horizontal(
                Label(summary.workspace_key, classes="ws-key"),
                Label(summary.name, classes="ws-name"),
                Label(summary.description or "—", classes="ws-desc"),
            )
        )
        self.summary = summary


class WorkspacesPanel(Container):
    DEFAULT_CSS = _CSS_PATH.read_text()

    def compose(self) -> ComposeResult:
        with Vertical():
            with Horizontal(classes="panel-header"):
                yield I18nLabel("workspaces_heading", classes="panel-heading")
                yield I18nButton(
                    key="create_workspace_btn",
                    id="create_btn",
                    classes="-success",
                )
            yield I18nLabel(
                "workspaces_empty",
                id="empty_state",
                classes="empty-state",
            )
            yield ListView(id="ws_list")

    def on_mount(self) -> None:
        self.refresh_data()

    @work(exclusive=True, group="ws_list")
    async def refresh_data(self) -> None:
        try:
            items = await WorkspaceAPI(self.app.client).list_my()
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Workspace list failed: %s", e)
            self.app.notify(
                i18n.get("workspace_create_failed", reason=str(e)),
                severity="error",
            )
            return
        self._apply_items(items)

    def _apply_items(self, items: list[WorkspaceSummary]) -> None:
        list_view = self.query_one("#ws_list", ListView)
        empty = self.query_one("#empty_state", I18nLabel)
        list_view.clear()
        if not items:
            list_view.display = False
            empty.display = True
            return
        empty.display = False
        list_view.display = True
        for item in items:
            list_view.append(_WorkspaceListItem(item))

    @on(Button.Pressed, "#create_btn")
    def _on_create_pressed(self) -> None:
        self._open_create_modal()

    @work
    async def _open_create_modal(self) -> None:
        result = await self.app.push_screen_wait(CreateWorkspaceModal())
        if result:
            self.refresh_data()

    @on(ListView.Selected)
    def _on_selected(self, event: ListView.Selected) -> None:
        if not isinstance(event.item, _WorkspaceListItem):
            return
        key = event.item.summary.workspace_key
        self.app.notify(i18n.get("workspace_selected_todo", key=key), timeout=2)
