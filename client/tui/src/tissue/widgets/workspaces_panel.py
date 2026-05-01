import logging
import re
from pathlib import Path

from textual import events, on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.message import Message
from textual.widgets import (
    Button,
    Input,
    Label,
    ListItem,
    ListView,
    Select,
    Static,
    TabbedContent,
    TabPane,
    TextArea,
)
from textual.widgets._select import SelectCurrent

from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.workspace import WorkspaceAPI
from tissue.i18n.manager import i18n
from tissue.models.workspace import CreateWorkspaceRequest, WorkspaceSummary
from tissue.widgets.i18n_widgets import I18nButton, I18nInput, I18nLabel
from tissue.widgets.modal_input import ModalInput

WS_KEY_PATTERN = re.compile(r"^[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$")

log = logging.getLogger(__name__)

_CSS_PATH = Path(__file__).parent / "css" / "workspaces_panel.tcss"


class _ClickableText(Static, can_focus=True):
    class Pressed(Message):
        def __init__(self, sender: "_ClickableText") -> None:
            super().__init__()
            self._sender = sender

        @property
        def control(self) -> "_ClickableText":
            return self._sender

    def __init__(self, i18n_key: str, shortcut: str | None = None, **kwargs):
        super().__init__(i18n.get(i18n_key), **kwargs)
        self._i18n_key = i18n_key
        if shortcut:
            self.border_title = f"({shortcut})"

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh)

    def _refresh(self) -> None:
        self.update(i18n.get(self._i18n_key))

    def on_click(self, event: events.Click) -> None:
        self.post_message(self.Pressed(self))
        event.stop()

    def on_key(self, event: events.Key) -> None:
        if event.key in ("enter", "space"):
            self.post_message(self.Pressed(self))
            event.stop()


class _WorkspaceListItem(ListItem):
    def __init__(self, summary: WorkspaceSummary, index: int) -> None:
        super().__init__(
            Horizontal(
                Label(str(index), classes="col-num"),
                Label(summary.workspace_key, classes="col-key"),
                Label(summary.name, classes="col-name"),
                Label(self._status_text(summary), classes="col-status"),
                Label(
                    summary.joined_at.strftime("%Y-%m-%d")
                    if summary.joined_at
                    else "-",
                    classes="col-joined",
                ),
            )
        )
        self.summary = summary
        if summary.deleted:
            self.add_class("-deleted")
        elif summary.archived:
            self.add_class("-archived")

    @staticmethod
    def _status_text(summary: WorkspaceSummary) -> str:
        if summary.deleted:
            return i18n.get("deleted_label")
        if summary.archived:
            return i18n.get("archived_label")
        return "-"


class _DetailField(Static):
    def __init__(self, title_key: str, **kwargs):
        super().__init__("", **kwargs)
        self._title_key = title_key

    def on_mount(self) -> None:
        self._refresh_title()
        i18n.subscribe(self._refresh_title)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_title)

    def _refresh_title(self) -> None:
        self.border_title = i18n.get(self._title_key)

    def set_value(self, text: str) -> None:
        self.update(text)


class _WorkspaceDetail(Container, can_focus=True):
    DEFAULT_CLASSES = "ws-detail"

    BINDINGS = [
        Binding("enter", "enter_workspace", show=False),
    ]

    class EnterPressed(Message):
        def __init__(self, summary: WorkspaceSummary) -> None:
            super().__init__()
            self.summary = summary

    def __init__(self) -> None:
        super().__init__()
        self._summary: WorkspaceSummary | None = None

    def compose(self) -> ComposeResult:
        yield I18nLabel("workspace_detail_empty", id="detail_empty")
        with Vertical(id="detail_info"):
            with Horizontal(classes="detail-row detail-row-top"):
                yield _DetailField(
                    "ws_col_key",
                    id="detail_key",
                    classes="detail-field detail-key",
                )
                yield _DetailField(
                    "ws_col_status",
                    id="detail_status",
                    classes="detail-field detail-status",
                )
                yield _ClickableText(
                    "enter_btn",
                    id="enter_btn",
                    classes="enter-btn",
                )
            yield _DetailField(
                "ws_col_name",
                id="detail_name",
                classes="detail-field detail-name",
            )
            yield _DetailField(
                "workspace_description_title",
                id="detail_desc",
                classes="detail-field detail-desc",
            )
            with Horizontal(classes="detail-row detail-row-bottom"):
                yield _DetailField(
                    "ws_field_role",
                    id="detail_role",
                    classes="detail-field detail-role",
                )
                yield _DetailField(
                    "ws_field_members",
                    id="detail_members",
                    classes="detail-field detail-members",
                )
                yield _DetailField(
                    "ws_col_joined",
                    id="detail_joined",
                    classes="detail-field detail-joined",
                )

    def on_mount(self) -> None:
        i18n.subscribe(self._refresh_i18n)
        self._refresh_title()
        self._show_empty()

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_i18n)

    def _refresh_title(self) -> None:
        self.border_title = i18n.get("workspace_detail_title")

    def show_summary(self, summary: WorkspaceSummary | None) -> None:
        self._summary = summary
        if summary is None:
            self._show_empty()
            return
        self.query_one("#detail_empty").display = False
        self.query_one("#detail_info").display = True
        self._refresh_i18n()

    def _show_empty(self) -> None:
        self.query_one("#detail_empty").display = True
        self.query_one("#detail_info").display = False

    def _refresh_i18n(self) -> None:
        self._refresh_title()
        s = self._summary
        if s is None:
            return
        self.query_one("#detail_key", _DetailField).set_value(s.workspace_key)
        self.query_one("#detail_status", _DetailField).set_value(
            _WorkspaceListItem._status_text(s)
        )
        self.query_one("#detail_name", _DetailField).set_value(s.name)
        self.query_one("#detail_desc", _DetailField).set_value(s.description or "—")
        self.query_one("#detail_role", _DetailField).set_value(s.my_role or "-")
        count = s.member_count
        self.query_one("#detail_members", _DetailField).set_value(
            str(count) if count is not None else "-"
        )
        joined = s.joined_at
        self.query_one("#detail_joined", _DetailField).set_value(
            joined.strftime("%Y-%m-%d") if joined else "-"
        )

    def action_enter_workspace(self) -> None:
        if self._summary is not None:
            self.post_message(self.EnterPressed(self._summary))

    @on(_ClickableText.Pressed, "#enter_btn")
    def _on_enter_button(self, event: _ClickableText.Pressed) -> None:
        event.stop()
        if self._summary is not None:
            self.post_message(self.EnterPressed(self._summary))


class _CreateWorkspaceForm(Container):
    DEFAULT_CLASSES = "ws-create-form"

    class Created(Message):
        def __init__(self, workspace_key: str) -> None:
            super().__init__()
            self.workspace_key = workspace_key

    def compose(self) -> ComposeResult:
        yield I18nInput(
            placeholder_key="workspace_key_placeholder",
            title_key="workspace_key_title",
            id="ws_key",
            classes="form-field",
        )
        yield I18nLabel("", id="ws_key_status", classes="status-msg")
        yield I18nInput(
            placeholder_key="workspace_name_placeholder",
            title_key="workspace_name_title",
            id="ws_name",
            classes="form-field",
        )
        yield I18nLabel("", id="ws_name_status", classes="status-msg")
        yield TextArea(id="ws_description", classes="form-field-textarea")
        yield I18nLabel("", id="ws_description_status", classes="status-msg")
        yield I18nButton(key="save_btn", id="save_btn", classes="-success")

    def on_mount(self) -> None:
        self._refresh_title()
        i18n.subscribe(self._refresh_title)

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_title)

    def _refresh_title(self) -> None:
        self.border_title = i18n.get("create_workspace_title")
        self.query_one("#ws_description", TextArea).border_title = i18n.get(
            "workspace_description_title"
        )

    def reset(self) -> None:
        for fid in ("ws_key", "ws_name"):
            self.query_one(f"#{fid}", ModalInput).value = ""
        self.query_one("#ws_description", TextArea).text = ""
        self._clear_all_status()

    def _set_status(
        self,
        input_id: str,
        label_id: str,
        key: str | None,
        is_error: bool = False,
    ) -> None:
        inp = self.query_one(input_id)
        lbl = self.query_one(label_id, I18nLabel)
        inp.remove_class("error", "success")
        lbl.remove_class("error", "success")
        if not key:
            lbl.clear_i18n()
            return
        lbl.set_i18n_key(key)
        cls = "error" if is_error else "success"
        inp.add_class(cls)
        lbl.add_class(cls)

    def _clear_all_status(self) -> None:
        for fid in ("ws_key", "ws_name", "ws_description"):
            self._set_status(f"#{fid}", f"#{fid}_status", None)

    @on(Input.Changed)
    def _on_changed(self, event: Input.Changed) -> None:
        ident = event.input.id
        if ident in ("ws_key", "ws_name", "ws_description"):
            self._set_status(f"#{ident}", f"#{ident}_status", None)

    @on(Input.Submitted)
    @on(Button.Pressed, "#save_btn")
    def _on_save(self) -> None:
        self._clear_all_status()
        ws_key = self.query_one("#ws_key", ModalInput).value.strip()
        name = self.query_one("#ws_name", ModalInput).value.strip()
        description = self.query_one("#ws_description", TextArea).text.strip()

        has_error = False
        if (
            not ws_key
            or not WS_KEY_PATTERN.match(ws_key)
            or not (3 <= len(ws_key) <= 22)
        ):
            self._set_status(
                "#ws_key", "#ws_key_status", "workspace_key_invalid", is_error=True
            )
            has_error = True
        if not name or not (2 <= len(name) <= 50):
            self._set_status(
                "#ws_name", "#ws_name_status", "workspace_name_invalid", is_error=True
            )
            has_error = True
        if len(description) > 255:
            self._set_status(
                "#ws_description",
                "#ws_description_status",
                "workspace_description_too_long",
                is_error=True,
            )
            has_error = True
        if has_error:
            return

        self._do_create(ws_key, name, description or None)

    @work(exclusive=True)
    async def _do_create(self, ws_key: str, name: str, description: str | None) -> None:
        try:
            await WorkspaceAPI(self.app.client).create(
                CreateWorkspaceRequest(
                    workspace_key=ws_key, name=name, description=description
                )
            )
        except ApiResponseError as e:
            log.warning("Workspace create failed: %s", e)
            if e.status_code == 409 or e.code == "WORKSPACE_KEY_CONFLICT":
                self._set_status(
                    "#ws_key",
                    "#ws_key_status",
                    "api_error.WORKSPACE_KEY_CONFLICT",
                    is_error=True,
                )
                return
            reason = e.code or f"HTTP {e.status_code}"
            self.app.notify(
                i18n.get("workspace_create_failed", reason=reason),
                severity="error",
            )
            return
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Workspace create error: %s", e)
            self.app.notify(
                i18n.get("workspace_create_failed", reason=str(e)),
                severity="error",
            )
            return

        self.app.notify(i18n.get("workspace_created", workspace_key=ws_key), timeout=2)
        self.post_message(self.Created(ws_key))


class WorkspacesPanel(Container):
    DEFAULT_CSS = _CSS_PATH.read_text()

    BINDINGS = [
        Binding("c", "create_workspace", "create workspace", priority=True),
        Binding("slash", "focus_search", "search", show=False),
        Binding("f", "focus_filter", "filter", show=False),
        Binding("s", "focus_sort", "sort", show=False),
    ]

    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._all_items: list[WorkspaceSummary] = []
        self._search_text: str = ""
        self._status_filter: str = "all"
        self._sort_value: str = "joined_desc"

    def action_create_workspace(self) -> None:
        self.query_one("#ws_detail_tabs", TabbedContent).active = "tab_create"

    def action_focus_search(self) -> None:
        self.query_one("#ws_search", Input).focus()

    def action_focus_filter(self) -> None:
        self.query_one("#ws_filter", Select).focus()

    def action_focus_sort(self) -> None:
        self.query_one("#ws_sort", Select).focus()

    def compose(self) -> ComposeResult:
        with Vertical():
            yield I18nLabel(
                "workspaces_empty",
                id="empty_state",
                classes="empty-state",
            )
            with Horizontal(id="ws_main"):
                with Vertical(id="ws_left"):
                    with Horizontal(classes="ws-list-toolbar"):
                        yield Input(
                            placeholder=i18n.get("ws_search_placeholder"),
                            id="ws_search",
                            classes="ws-search",
                        )
                        yield Select(
                            [
                                (i18n.get("ws_filter_all"), "all"),
                                (i18n.get("ws_filter_active"), "active"),
                                (i18n.get("ws_filter_archived"), "archived"),
                                (i18n.get("ws_filter_deleted"), "deleted"),
                            ],
                            value="all",
                            allow_blank=False,
                            id="ws_filter",
                            classes="ws-filter",
                        )
                        yield Select(
                            [
                                (i18n.get("ws_sort_joined_desc"), "joined_desc"),
                                (i18n.get("ws_sort_joined_asc"), "joined_asc"),
                                (i18n.get("ws_sort_key_asc"), "key_asc"),
                                (i18n.get("ws_sort_key_desc"), "key_desc"),
                                (i18n.get("ws_sort_name_asc"), "name_asc"),
                                (i18n.get("ws_sort_name_desc"), "name_desc"),
                            ],
                            value="joined_desc",
                            allow_blank=False,
                            id="ws_sort",
                            classes="ws-sort",
                        )
                    with Vertical(id="ws_list_wrap"):
                        with Horizontal(classes="ws-list-header"):
                            yield I18nLabel("ws_col_num", classes="col-num")
                            yield I18nLabel("ws_col_key", classes="col-key")
                            yield I18nLabel("ws_col_name", classes="col-name")
                            yield I18nLabel("ws_col_status", classes="col-status")
                            yield I18nLabel("ws_col_joined", classes="col-joined")
                        yield ListView(id="ws_list")
                with TabbedContent(id="ws_detail_tabs"):
                    with TabPane(i18n.get("ws_tab_detail"), id="tab_detail"):
                        yield _WorkspaceDetail()
                    with TabPane(i18n.get("ws_tab_create"), id="tab_create"):
                        yield _CreateWorkspaceForm()
            with Container(id="ws_actions"):
                with Horizontal(classes="actions-row"):
                    yield _ClickableText(
                        "ws_action_edit",
                        id="action_edit",
                        classes="action-btn",
                    )
                    yield _ClickableText(
                        "ws_action_archive",
                        id="action_archive",
                        classes="action-btn",
                    )
                    yield _ClickableText(
                        "ws_action_delete",
                        id="action_delete",
                        classes="action-btn",
                    )
                    yield _ClickableText(
                        "ws_action_leave",
                        id="action_leave",
                        classes="action-btn",
                    )

    def on_mount(self) -> None:
        self._refresh_title()
        i18n.subscribe(self._refresh_title)
        self.refresh_data()

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._refresh_title)

    def _refresh_title(self) -> None:
        self.query_one("#ws_list_wrap").border_title = i18n.get("ws_list_title")
        self.query_one("#ws_actions").border_title = i18n.get("ws_actions_title")
        search = self.query_one("#ws_search", Input)
        search.border_title = i18n.get("ws_search_title")
        search.border_subtitle = "(/)"
        filter_sc = self.query_one("#ws_filter").query_one(SelectCurrent)
        filter_sc.border_title = i18n.get("ws_filter_title")
        filter_sc.border_subtitle = "(f)"
        sort_sc = self.query_one("#ws_sort").query_one(SelectCurrent)
        sort_sc.border_title = i18n.get("ws_sort_title")
        sort_sc.border_subtitle = "(s)"

    def focus_default(self) -> None:
        items = list(self.query(_WorkspaceListItem))
        if items:
            self.query_one("#ws_list", ListView).focus()
            return
        try:
            self.query_one("#create_btn", _ClickableText).focus()
        except Exception:
            pass

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
        # TODO: backend currently doesn't send archived/deleted; mock for preview
        for idx, item in enumerate(items):
            if idx == 1 and len(items) > 1:
                item.archived = True
            elif idx == 2 and len(items) > 2:
                item.deleted = True
        self._all_items = items
        self._refresh_view()

    def _refresh_view(self) -> None:
        items = self._filter_and_sort(self._all_items)
        ws_list = self.query_one("#ws_list", ListView)
        main = self.query_one("#ws_main", Horizontal)
        empty = self.query_one("#empty_state", I18nLabel)
        detail = self.query_one(_WorkspaceDetail)
        ws_list.clear()
        if not self._all_items:
            main.display = False
            empty.display = True
            detail.show_summary(None)
            return
        empty.display = False
        main.display = True
        for idx, item in enumerate(items, start=1):
            ws_list.append(_WorkspaceListItem(item, idx))
        detail.show_summary(items[0] if items else None)

    def _filter_and_sort(self, items: list[WorkspaceSummary]) -> list[WorkspaceSummary]:
        result = items
        if self._status_filter == "active":
            result = [i for i in result if not i.archived and not i.deleted]
        elif self._status_filter == "archived":
            result = [i for i in result if i.archived]
        elif self._status_filter == "deleted":
            result = [i for i in result if i.deleted]
        q = self._search_text.lower().strip()
        if q:
            result = [
                i for i in result if q in i.workspace_key.lower() or q in i.name.lower()
            ]
        result = sorted(result, key=self._sort_key_func(), reverse=self._sort_desc())
        return result

    def _sort_key_func(self):
        v = self._sort_value
        if v.startswith("joined"):
            return lambda i: i.joined_at or i.created_at
        if v.startswith("key"):
            return lambda i: i.workspace_key.lower()
        return lambda i: i.name.lower()

    def _sort_desc(self) -> bool:
        return self._sort_value.endswith("_desc")

    @on(Input.Changed, "#ws_search")
    def _on_search_changed(self, event: Input.Changed) -> None:
        self._search_text = event.value
        self._refresh_view()

    @on(Select.Changed, "#ws_filter")
    def _on_filter_changed(self, event: Select.Changed) -> None:
        self._status_filter = str(event.value)
        self._refresh_view()

    @on(Select.Changed, "#ws_sort")
    def _on_sort_changed(self, event: Select.Changed) -> None:
        self._sort_value = str(event.value)
        self._refresh_view()

    @on(_ClickableText.Pressed, ".action-btn")
    def _on_action_pressed(self, event: _ClickableText.Pressed) -> None:
        event.stop()
        self.app.notify(i18n.get("feature_todo"), timeout=2)

    @on(_CreateWorkspaceForm.Created)
    def _on_workspace_created(self, event: _CreateWorkspaceForm.Created) -> None:
        event.stop()
        self.query_one(_CreateWorkspaceForm).reset()
        self.query_one("#ws_detail_tabs", TabbedContent).active = "tab_detail"
        self.refresh_data()

    @on(ListView.Highlighted, "#ws_list")
    def _on_highlighted(self, event: ListView.Highlighted) -> None:
        detail = self.query_one(_WorkspaceDetail)
        if isinstance(event.item, _WorkspaceListItem):
            detail.show_summary(event.item.summary)
        else:
            detail.show_summary(None)

    @on(ListView.Selected, "#ws_list")
    def _on_selected(self, event: ListView.Selected) -> None:
        if isinstance(event.item, _WorkspaceListItem):
            self._enter_workspace(event.item.summary)

    @on(_WorkspaceDetail.EnterPressed)
    def _on_detail_enter(self, event: _WorkspaceDetail.EnterPressed) -> None:
        self._enter_workspace(event.summary)

    def _enter_workspace(self, summary: WorkspaceSummary) -> None:
        self.app.notify(
            i18n.get("workspace_selected_todo", workspace_key=summary.workspace_key),
            timeout=2,
        )
