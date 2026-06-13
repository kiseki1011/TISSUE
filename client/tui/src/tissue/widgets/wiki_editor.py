from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal, VerticalScroll
from textual.css.query import NoMatches
from textual.message import Message
from textual.widgets import Button, Input, Label, Markdown, Select, TextArea

_TITLE_MIN, _TITLE_MAX = 1, 200
_CONTENT_MAX = 100000
_REASON_MAX = 255


class WikiEditor(Container):
    class Saved(Message):
        def __init__(
            self,
            *,
            title: str,
            content: str,
            create_mode: str | None,
            version_update_type: str | None,
            edit_reason: str | None,
        ) -> None:
            super().__init__()
            self.title = title
            self.content = content
            self.create_mode = create_mode  # "top" | "child" | "parent" | None
            self.version_update_type = version_update_type  # MAJOR/MINOR/PATCH | None
            self.edit_reason = edit_reason

    class Cancelled(Message):
        pass

    DEFAULT_CSS = """
    WikiEditor {
        width: 100%;
        height: 1fr;
    }
    WikiEditor .field-label {
        width: 100%;
        color: $text-muted;
        padding: 1 0 0 1;
    }
    WikiEditor .input-field {
        width: 100%;
    }
    WikiEditor #wiki-editor-mode, WikiEditor #wiki-editor-updatetype {
        width: 100%;
        margin-bottom: 1;
    }
    WikiEditor #wiki-editor-body {
        width: 100%;
        height: 1fr;
        margin-top: 1;
    }
    WikiEditor #wiki-editor-content {
        width: 100%;
        height: 1fr;
    }
    WikiEditor .wiki-editor-preview {
        width: 100%;
        height: 1fr;
    }
    WikiEditor #wiki-editor-buttons {
        width: 100%;
        height: auto;
        align-horizontal: right;
        margin-top: 1;

        Button {
            margin-left: 1;
            min-width: 12;
        }
    }
    """

    def __init__(
        self,
        *,
        mode: str,  # "create" | "edit"
        title: str = "",
        content: str = "",
        parent_title: str | None = None,
        allow_child: bool = False,
        allow_parent: bool = False,
    ) -> None:
        super().__init__()
        self._mode = mode
        self._init_title = title
        self._draft_content = content
        self._parent_title = parent_title
        self._allow_child = allow_child
        self._allow_parent = allow_parent
        self._preview = False

    def compose(self) -> ComposeResult:
        if self._mode == "create" and (self._allow_child or self._allow_parent):
            options = [("Top-level document", "top")]
            if self._allow_child:
                options.append(
                    (
                        f"Child of: {self._parent_title or '-'}",
                        "child",
                    )
                )
            if self._allow_parent:
                options.append(
                    (
                        f"Parent of: {self._parent_title or '-'}",
                        "parent",
                    )
                )
            default = "child" if self._allow_child else "top"
            yield Label("Location", classes="field-label")
            yield Select(
                options, value=default, allow_blank=False, id="wiki-editor-mode"
            )

        title = Input(
            value=self._init_title,
            placeholder="Document title",
            id="wiki-editor-title",
            classes="input-field",
        )
        title.border_title = "Title"
        yield title

        if self._mode == "edit":
            yield Label("Version bump", classes="field-label")
            yield Select(
                [
                    ("Patch (x.x.+1)", "PATCH"),
                    ("Minor (x.+1.0)", "MINOR"),
                    ("Major (+1.0.0)", "MAJOR"),
                ],
                value="PATCH",
                allow_blank=False,
                id="wiki-editor-updatetype",
            )
            reason = Input(
                placeholder="What changed?",
                id="wiki-editor-reason",
                classes="input-field",
            )
            reason.border_title = "Edit reason (optional)"
            yield reason

        body = Container(self._build_editor(), id="wiki-editor-body")
        yield body
        yield Label("", id="wiki-editor-status", classes="status-msg")
        yield Horizontal(
            Button("Preview", id="wiki-editor-preview-btn"),
            Button("Cancel", id="wiki-editor-cancel-btn"),
            Button(
                self._save_label(),
                id="wiki-editor-save-btn",
                classes="-btn-success",
            ),
            id="wiki-editor-buttons",
        )

    def _build_editor(self) -> TextArea:
        # Preview toggles markdown rendering.
        editor = TextArea(
            self._draft_content,
            id="wiki-editor-content",
            soft_wrap=True,
            tab_behavior="focus",
            show_line_numbers=False,
        )
        editor.border_title = "Content"
        return editor

    def _save_label(self) -> str:
        return "Create" if self._mode == "create" else "Save"

    def on_mount(self) -> None:
        self.query_one("#wiki-editor-title", Input).focus()

    @on(TextArea.Changed, "#wiki-editor-content")
    def _on_content_changed(self, event: TextArea.Changed) -> None:
        self._draft_content = event.text_area.text

    @on(Button.Pressed, "#wiki-editor-preview-btn")
    async def _on_toggle_preview(self) -> None:
        body = self.query_one("#wiki-editor-body", Container)
        toggle = self.query_one("#wiki-editor-preview-btn", Button)
        if not self._preview:
            # Capture the draft from the live editor before swapping it out.
            self._draft_content = self._current_content()
            await body.remove_children()
            await body.mount(
                VerticalScroll(
                    Markdown(self._draft_content), classes="wiki-editor-preview"
                )
            )
            self._preview = True
            toggle.label = "Edit"
        else:
            await body.remove_children()
            await body.mount(self._build_editor())
            self._preview = False
            toggle.label = "Preview"

    def _current_content(self) -> str:
        if self._preview:
            return self._draft_content
        try:
            return self.query_one("#wiki-editor-content", TextArea).text
        except NoMatches:
            return self._draft_content

    @on(Button.Pressed, "#wiki-editor-cancel-btn")
    def _on_cancel(self) -> None:
        self.post_message(self.Cancelled())

    @on(Button.Pressed, "#wiki-editor-save-btn")
    def _on_save(self) -> None:
        title = self.query_one("#wiki-editor-title", Input).value.strip()
        content = self._current_content()
        if not title or not (_TITLE_MIN <= len(title) <= _TITLE_MAX):
            self._set_status("1-200 characters")
            return
        if not content.strip():
            self._set_status("Content is required.")
            return
        if len(content) > _CONTENT_MAX:
            self._set_status("Content is too long (max 100000).")
            return

        edit_reason: str | None = None
        if self._mode == "edit":
            reason = self.query_one("#wiki-editor-reason", Input).value.strip()
            edit_reason = reason[:_REASON_MAX] or None

        self.post_message(
            self.Saved(
                title=title,
                content=content,
                create_mode=self._selected_create_mode(),
                version_update_type=self._selected_update_type(),
                edit_reason=edit_reason,
            )
        )

    def _selected_create_mode(self) -> str | None:
        if self._mode != "create":
            return None
        try:
            value = self.query_one("#wiki-editor-mode", Select).value
        except NoMatches:
            return "top"  # only top-level option offered
        return value if isinstance(value, str) else "top"

    def _selected_update_type(self) -> str | None:
        if self._mode != "edit":
            return None
        value = self.query_one("#wiki-editor-updatetype", Select).value
        return value if isinstance(value, str) else "PATCH"

    def _set_status(self, message: str) -> None:
        label = self.query_one("#wiki-editor-status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message)
        label.add_class("-error")
