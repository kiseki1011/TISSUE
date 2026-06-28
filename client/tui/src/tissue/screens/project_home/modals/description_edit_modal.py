from __future__ import annotations

import logging

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Markdown, Static, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)


class DescriptionEditModal(TissueModal["bool | None"]):
    """Edit an issue's Markdown description with an Edit/Preview toggle.

    Closes with True after a save so the caller can redraw, None on cancel.
    """

    CSS_PATH = "description_edit_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
        Binding("ctrl+t", "toggle_preview", "preview"),
    ]

    def __init__(self, *, issue_key: str, current_content: str | None) -> None:
        super().__init__()
        self._issue_key = issue_key
        self._current_content = current_content or ""
        self._previewing = False

    def compose(self) -> ComposeResult:
        with Container(id="ded-dialog", classes="dialog"):
            yield TextArea(self._current_content, id="ded-editor", language="markdown")
            yield Markdown("", id="ded-preview")
            yield Static("", id="ded-status", classes="status-msg")
            with Horizontal(id="ded-actions"):
                yield Button("Preview", id="ded-preview-btn")
                yield Button("Cancel", id="ded-cancel", classes="-btn-error")
                yield Button("Save", id="ded-save", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#ded-dialog", Container)
        dialog.border_title = "Edit Description"
        dialog.border_subtitle = "Ctrl+T preview · Esc to cancel"
        self.query_one("#ded-preview", Markdown).display = False
        self.query_one("#ded-editor", TextArea).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#ded-preview-btn")
    def _on_preview_btn(self, event: Button.Pressed) -> None:
        event.stop()
        self.action_toggle_preview()

    def action_toggle_preview(self) -> None:
        editor = self.query_one("#ded-editor", TextArea)
        preview = self.query_one("#ded-preview", Markdown)
        preview_button = self.query_one("#ded-preview-btn", Button)
        self._previewing = not self._previewing
        if self._previewing:
            self.run_worker(self._render_preview(editor.text), group="ded-preview")
            editor.display = False
            preview.display = True
            preview_button.label = "Edit"
        else:
            preview.display = False
            editor.display = True
            preview_button.label = "Preview"
            editor.focus()

    async def _render_preview(self, text: str) -> None:
        await self.query_one("#ded-preview", Markdown).update(text)

    @on(Button.Pressed, "#ded-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#ded-save")
    def _on_save(self, event: Button.Pressed) -> None:
        event.stop()
        self.run_worker(self._do_save(), exclusive=True, group="ded-save")

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        content = self.query_one("#ded-editor", TextArea).text
        try:
            await client.issues.update_common_fields(self._issue_key, content=content)
        except TissueApiError as error:
            self.query_one("#ded-status", Static).update(
                getattr(error, "detail", None) or str(error) or "Update failed."
            )
            return
        self.dismiss(True)
