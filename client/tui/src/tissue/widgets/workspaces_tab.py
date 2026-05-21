from textual import on
from textual.app import ComposeResult
from textual.containers import Container
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.api.generated.models.workspace_create_response import (
    WorkspaceCreateResponse,
)
from tissue.api.generated.models.workspace_summary_response import (
    WorkspaceSummaryResponse,
)
from tissue.i18n.manager import i18n
from tissue.screens.workspace_home import WorkspaceHomeScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView


class WorkspacesTab(Widget):
    """Workspaces tab content.

    Intentionally does not implement Refreshable. The workspaces cache is
    kept in sync as a side-effect of the relevant actions (create, accept invitation).
    """

    DEFAULT_CSS = """
    WorkspacesTab {
        width: 100%;
        height: 100%;
    }
    """

    def compose(self) -> ComposeResult:
        yield TableDetailSplitView[WorkspaceSummaryResponse](
            id="workspaces-split",
            columns=self._columns(),
            row_builder=self._row,
            detail_renderer=self._render_detail,
            items=self._items(),
            table_title=i18n.get("home_workspaces_table_title"),
            detail_title=i18n.get("home_detail_title"),
        )

    def handle_created(self, response: WorkspaceCreateResponse | None) -> None:
        """Called by the parent screen after the create-workspace modal closes."""
        if response is None or response.workspace_key is None:
            return
        self.query_one("#workspaces-split", TableDetailSplitView).populate(
            self._items()
        )
        created = next(
            (ws for ws in self._items() if ws.workspace_key == response.workspace_key),
            None,
        )
        if created is not None:
            self.app.push_screen(WorkspaceHomeScreen(created))

    @on(DataTable.RowSelected)
    def _on_row_selected(self, event: DataTable.RowSelected) -> None:
        """Enter on a workspace row opens its home screen."""
        event_owner = event.data_table.query_ancestor(TableDetailSplitView)
        if event_owner is None or event_owner.id != "workspaces-split":
            return
        items = self._items()
        if 0 <= event.cursor_row < len(items):
            self.app.push_screen(WorkspaceHomeScreen(items[event.cursor_row]))

    def _columns(self) -> list[Column]:
        return [
            Column("no", i18n.get("home_col_no"), 2),
            Column("key", i18n.get("home_col_workspace_key"), 16),
            Column("name", i18n.get("home_col_name"), 24),
            Column("status", i18n.get("home_col_status"), 12),
            Column("created", i18n.get("home_col_created"), 18),
        ]

    def _row(self, idx: int, ws: WorkspaceSummaryResponse) -> list[str]:
        return [
            str(idx),
            ws.workspace_key or "-",
            ws.name or "-",
            _format_status(ws.status),
            format_relative(ws.created_at),
        ]

    def _render_detail(
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
            (i18n.get("home_workspace_role"), ws.my_role or "-"),
            (i18n.get("home_workspace_status"), _format_status(ws.status)),
            (
                i18n.get("home_workspace_members"),
                str(ws.member_count) if ws.member_count is not None else "-",
            ),
            (i18n.get("home_workspace_created"), format_relative(ws.created_at)),
        ]
        for key, value in rows:
            content.mount(detail_row(key, value))

    def _items(self) -> list[WorkspaceSummaryResponse]:
        client = self.app.client
        return list(client.workspaces.cached or []) if client is not None else []


_STATUS_TO_I18N = {
    "ACTIVE": "home_workspace_status_active",
    "ARCHIVED": "home_workspace_status_archived",
    "DELETED": "home_workspace_status_deleted",
}

# Textual markup styles applied to the status label so the table cell + detail
# row both render with the right emphasis. DELETED gets $error so the row
# stands out at a glance even before the user reads the label.
_STATUS_STYLES = {
    "ACTIVE": "",
    "ARCHIVED": "$text-muted",
    "DELETED": "$error",
}


def _format_status(status: str | None) -> str:
    """Localized + colored status label for a workspace.

    Falls back to '-' if status is missing (defensive against older
    backends that may not populate the field yet).
    """
    if status is None:
        return "-"
    i18n_key = _STATUS_TO_I18N.get(status)
    label = i18n.get(i18n_key) if i18n_key else status
    style = _STATUS_STYLES.get(status, "")
    return f"[{style}]{label}[/{style}]" if style else label
