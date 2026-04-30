import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.screen import ModalScreen
from textual.widgets import Button, Input

from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.workspace import WorkspaceAPI
from tissue.i18n.manager import i18n
from tissue.models.workspace import CreateWorkspaceRequest
from tissue.widgets.i18n_widgets import I18nButton, I18nContainer, I18nInput, I18nLabel
from tissue.widgets.modal_input import ModalInput

log = logging.getLogger(__name__)

WS_KEY_PATTERN = re.compile(r"^[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$")


class CreateWorkspaceModal(ModalScreen[bool | None]):
    CSS_PATH = ["css/_buttons.tcss", "css/create_workspace.tcss"]

    BINDINGS = [
        Binding("escape", "close", show=False),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
    ]

    def compose(self) -> ComposeResult:
        yield I18nContainer(
            I18nInput(
                placeholder_key="workspace_key_placeholder",
                title_key="workspace_key_title",
                id="ws_key",
                classes="input-field",
            ),
            I18nLabel("", id="ws_key_status", classes="status-msg"),
            I18nInput(
                placeholder_key="workspace_name_placeholder",
                title_key="workspace_name_title",
                id="ws_name",
                classes="input-field",
            ),
            I18nLabel("", id="ws_name_status", classes="status-msg"),
            I18nInput(
                placeholder_key="workspace_description_placeholder",
                title_key="workspace_description_title",
                id="ws_description",
                classes="input-field",
            ),
            I18nLabel("", id="ws_description_status", classes="status-msg"),
            I18nButton(key="save_btn", id="save_btn", classes="-success"),
            id="create-workspace-dialog",
            title_key="create_workspace_title",
        )

    def on_mount(self) -> None:
        self.query_one("#ws_key", ModalInput).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    def action_nav_down(self) -> None:
        self.focus_next()

    def action_nav_up(self) -> None:
        self.focus_previous()

    def _set_status(
        self,
        input_id: str,
        label_id: str,
        key: str | None,
        is_error: bool = False,
        **fmt,
    ) -> None:
        inp = self.query_one(input_id, ModalInput)
        lbl = self.query_one(label_id, I18nLabel)
        inp.remove_class("error", "success")
        lbl.remove_class("error", "success")
        if not key:
            lbl.clear_i18n()
            return
        lbl.set_i18n_key(key, **fmt)
        cls = "error" if is_error else "success"
        inp.add_class(cls)
        lbl.add_class(cls)

    def _clear_all_status(self) -> None:
        for ident in ("ws_key", "ws_name", "ws_description"):
            self._set_status(f"#{ident}", f"#{ident}_status", None)

    @on(Input.Changed)
    def on_changed(self, event: Input.Changed) -> None:
        ident = event.input.id
        if ident in ("ws_key", "ws_name", "ws_description"):
            self._set_status(f"#{ident}", f"#{ident}_status", None)

    @on(Input.Submitted)
    @on(Button.Pressed, "#save_btn")
    def on_save(self) -> None:
        self._clear_all_status()
        ws_key = self.query_one("#ws_key", ModalInput).value.strip()
        name = self.query_one("#ws_name", ModalInput).value.strip()
        description = self.query_one("#ws_description", ModalInput).value.strip()

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

        self.app.notify(i18n.get("workspace_created", key=ws_key), timeout=2)
        self.dismiss(True)
