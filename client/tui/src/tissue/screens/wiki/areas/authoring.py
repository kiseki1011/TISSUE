from __future__ import annotations

import logging
from typing import cast

from textual import on
from textual.containers import Horizontal
from textual.content import Content
from textual.css.query import NoMatches
from textual.widgets import (
    Button,
    Input,
    Label,
    Rule,
    Select,
    Static,
    TextArea,
)

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.wiki_document_detail import WikiDocumentDetail
from tissue.domain.wiki.drafts import Draft
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.constants import (
    _CONTENT_MAX,
    _DEFAULT_VERSION_BUMP,
    _MAX_TAGS,
    _PARENT_TITLE_LIMIT,
    _ROOT_PARENT,
    _TITLE_MAX,
)
from tissue.screens.wiki.modals import (
    TagChoice,
    TagPickerModal,
)
from tissue.screens.wiki.rendering import (
    _tags_text,
)
from tissue.screens.wiki.tag_colors import tag_chip_style, tag_fg
from tissue.screens.wiki.widgets import _WikiViewer

log = logging.getLogger(__name__)


class AuthoringMixin(WikiScreenBase):
    """The draft form (new document or edit-in-place): preview toggle, tag picking,
    saving/publishing, and version bumping."""

    @on(Button.Pressed, "#wiki-newdoc-btn")
    def _on_new_doc_pressed(self, event: Button.Pressed) -> None:
        if self._editing:
            self.app.notify("You're already writing a draft.", severity="warning")
            return
        self._enter_authoring(None)

    @on(Button.Pressed, "#wiki-edit-btn")
    def _on_edit_pressed(self, event: Button.Pressed) -> None:
        # Enter edit mode for the open document, reusing the authoring form. The
        # button is already disabled while the doc is locked (see
        # _update_lock_button); the guard here is belt-and-suspenders for the
        # (locked-after-open) race.
        event.stop()
        doc = self._current_doc
        if doc is None or self._current_doc_id is None:
            return
        if doc.locked:
            self.app.notify(
                "This document is locked. Unlock it to edit.", severity="warning"
            )
            return
        self._enter_editing(doc)

    @on(Button.Pressed, "#wiki-edit-cancel-btn")
    def _on_edit_cancel_pressed(self, event: Button.Pressed) -> None:
        self._exit_authoring()

    @on(Button.Pressed, "#wiki-preview-btn")
    async def _on_preview_pressed(self, event: Button.Pressed) -> None:
        await self._toggle_preview()

    async def action_toggle_preview(self) -> None:
        await self._toggle_preview()

    async def _toggle_preview(self) -> None:
        """Swap the draft body between the plain editor and a rendered Markdown
        preview. No-op outside authoring."""
        if not self._editing:
            return
        if self._preview:
            self._end_preview()
        else:
            await self._start_preview()

    async def _start_preview(self) -> None:
        try:
            editor = self.query_one("#wiki-editor", TextArea)
            preview = self.query_one("#wiki-preview", _WikiViewer)
        except NoMatches:
            return
        body = editor.text.strip() or "_(empty draft — nothing to preview yet)_"
        await preview.document.update(body)
        preview.scroll_home(animate=False)
        editor.display = False
        preview.display = True
        self._preview = True
        self._update_preview_button()

    def _end_preview(self) -> None:
        try:
            editor = self.query_one("#wiki-editor", TextArea)
            preview = self.query_one("#wiki-preview", _WikiViewer)
        except NoMatches:
            return
        preview.display = False
        editor.display = True
        self._preview = False
        self._update_preview_button()
        editor.focus()

    def _update_preview_button(self) -> None:
        try:
            btn = self.query_one("#wiki-preview-btn", Button)
        except NoMatches:
            return
        btn.label = "Edit" if self._preview else "Preview"

    @on(Button.Pressed, "#wiki-draft-save-btn")
    def _on_draft_save_pressed(self, event: Button.Pressed) -> None:
        draft = self._collect_draft()
        if draft is None:
            return
        try:
            self._draft_store().save(draft)
        except OSError as e:
            log.warning("Wiki: draft save failed: %s", e)
            self.app.notify("Couldn't save the draft.", severity="error")
            return
        # Now backed by a file, so subsequent draft-saves overwrite it in place.
        self._editing_draft = draft
        name = draft.path.name if draft.path else "draft"
        self.app.notify(f"Draft saved ({name}).")
        self._reload_drafts()

    @on(Button.Pressed, "#wiki-save-btn")
    def _on_save_pressed(self, event: Button.Pressed) -> None:
        # Edit mode (existing document) updates in place; new-doc authoring
        # creates and publishes a fresh document.
        if self._edit_target_id is not None:
            self.run_worker(self._save_edit(), exclusive=True, group="wiki-publish")
            return
        draft = self._collect_draft()
        if draft is None:
            return
        self.run_worker(self._publish(draft), exclusive=True, group="wiki-publish")

    def _tags_content(
        self, tags: list[tuple[str, str | None]], *, show_add: bool
    ) -> Content:
        """The Tags value as coloured pills, plus a clickable "+" (opens the tag
        manager) after the last pill when `show_add`. One Content so the label
        wraps it as a unit and the "+" follows the final tag."""
        content = Content("")
        for i, (name, colour) in enumerate(tags):
            if i:
                content += Content(" ")  # gap between pills
            pill = Content(f" {name} ")
            style = tag_chip_style(colour)
            content += pill.stylize(style) if style else pill
        if show_add:
            if tags:
                content += Content(" ")
            primary = self.app.theme_variables.get("primary") or "#0178d4"
            plus = Content(" + ").stylize(f"{tag_fg(primary)} on {primary}")
            content += plus.stylize("@click=screen.add_tags")
        return content

    def action_add_tags(self) -> None:
        # Fired by the inline "+" span's `@click=screen.add_tags` in the Tags row;
        # opens the tag manager for the open document. Applying the result
        # attaches/detaches against the server.
        doc = self._current_doc
        if doc is None:
            return
        initial: list[TagChoice] = [
            (t.name, t.color) for t in (doc.tags or []) if t.name
        ]
        self.app.push_screen(TagPickerModal(initial), self._on_tags_picked)

    def _on_tags_picked(self, result: list[TagChoice] | None) -> None:
        if result is None:  # cancelled
            return
        self.run_worker(self._apply_doc_tags(result), exclusive=True, group="wiki-tags")

    async def _apply_doc_tags(self, desired: list[TagChoice]) -> None:
        """Reconcile the open document's tags with `desired`: detach the ones the
        user removed, attach the ones they added, then re-render the Tags row.
        Detach BEFORE attach so swapping a tag on a full (5-tag) document never
        transiently exceeds the cap (→ 409)."""
        client = self.app.client
        doc = self._current_doc
        wiki_id = self._current_doc_id
        if client is None or doc is None or wiki_id is None:
            return
        current = {(t.name or "").casefold(): t for t in (doc.tags or []) if t.name}
        desired_cf = {name.casefold() for name, _ in desired}
        failed: list[str] = []
        for cf, tag in current.items():
            if cf not in desired_cf and tag.tag_id is not None:
                try:
                    await client.wiki.detach_tag(wiki_id, tag.tag_id)
                except TissueApiError as e:
                    log.warning("Wiki: detach tag %r failed: %s", tag.name, e)
                    failed.append(tag.name or "?")
        for name, color in desired:
            if name.casefold() not in current:
                try:
                    await client.wiki.attach_tag(wiki_id, name=name, color=color)
                except TissueApiError as e:
                    log.warning("Wiki: attach tag %r failed: %s", name, e)
                    failed.append(name)
        # Re-fetch so the row reflects the server's truth (canonical names,
        # find-or-create colours), but only if we're still on the same document.
        try:
            refreshed = await client.wiki.get_document(wiki_id)
        except TissueApiError as e:
            log.debug("Wiki: couldn't reload tags for %s: %s", wiki_id, e)
            refreshed = None
        if self._current_doc_id == wiki_id:
            if refreshed is not None:
                self._current_doc = refreshed
                self._render_meta_tags(
                    [(t.name or "", t.color) for t in (refreshed.tags or []) if t.name]
                )
            else:
                # The writes committed but the re-fetch failed; show the desired
                # set optimistically (instead of silently keeping the stale row)
                # and flag that it may be stale until the document is reopened.
                self._render_meta_tags(list(desired))
                self.app.notify(
                    "Tags updated, but couldn't refresh the view — reopen to confirm.",
                    severity="warning",
                )
        if failed:
            self.app.notify(
                f"Some tags couldn't be updated: {', '.join(failed)}",
                severity="warning",
            )

    def _render_meta_tags(self, tags: list[tuple[str, str | None]]) -> None:
        """Refresh just the Tags row value (keeps the version picker untouched —
        a full _render_meta would reset it). Re-includes the inline "+" per the
        current action-visibility state."""
        try:
            label = self.query_one("#wiki-meta-tags", Label)
        except NoMatches:
            return
        label.update(self._tags_content(tags, show_add=self._meta_actions_visible))

    def _update_draft_tags_display(self) -> None:
        try:
            label = self.query_one("#wiki-edit-tags-display", Label)
        except NoMatches:
            return
        # Colour from the live doc in edit mode; new-doc drafts carry only names.
        doc = self._current_doc
        colours: dict[str, str | None] = {}
        if doc is not None:
            colours = {(t.name or ""): t.color for t in (doc.tags or []) if t.name}
        label.update(
            _tags_text([(name, colours.get(name)) for name in self._draft_tags])
        )

    def _enter_authoring(self, draft: Draft | None) -> None:
        """Switch the content pane to the draft editor. `draft` pre-fills the
        form (continuing a saved draft); None starts blank."""
        # Resolve the editor widgets BEFORE mutating any state, so a missing
        # widget can't strand us in a half-entered editing mode (_editing=True
        # with the reader hidden but no editor shown — and `r` then suppressed).
        try:
            title = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return
        self._editing = True
        self._edit_target_id = None  # authoring a NEW document, not editing one
        self._editing_draft = draft
        self._draft_tags = list(draft.tags) if draft else []
        self._preview = False  # always start in edit mode
        self.refresh_bindings()  # `r` is suppressed while editing (can_refresh)
        for selector, kind in (
            ("#wiki-meta", Horizontal),
            ("#wiki-viewer", _WikiViewer),
            ("#wiki-preview", _WikiViewer),
            ("#wiki-placeholder", Static),
        ):
            self._set_display(selector, kind, False)
        title.value = draft.title if draft else ""
        editor.text = draft.body if draft else ""
        self._populate_parent_select()
        self._set_authoring_controls(editing=False)
        self._update_draft_tags_display()
        self._update_preview_button()
        self._set_display("#wiki-edit-meta", Horizontal, True)
        self._set_display("#wiki-meta-rule", Rule, True)
        self._set_display("#wiki-editor", TextArea, True)
        title.focus()

    def _enter_editing(self, doc: WikiDocumentDetail) -> None:
        """Switch the content pane to the editor pre-filled with the open
        document, to update it in place. Reuses the authoring form but: the
        parent picker becomes a version-bump picker, "Save draft" is hidden, and
        a save writes back via update_title / update_content."""
        try:
            title = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return
        doc_id = self._current_doc_id
        if doc_id is None:
            return
        self._editing = True
        self._edit_target_id = doc_id
        self._editing_draft = None
        # Pre-fill from the live document; remember the originals so a save only
        # writes the fields that actually changed.
        self._edit_original_title = doc.title or ""
        self._edit_original_body = doc.content or ""
        # The current tags are shown read-only here (they're managed via the
        # meta "+" in read mode); editing covers title + content only.
        self._draft_tags = [t.name for t in (doc.tags or []) if t.name]
        self._preview = False
        self.refresh_bindings()
        for selector, kind in (
            ("#wiki-meta", Horizontal),
            ("#wiki-viewer", _WikiViewer),
            ("#wiki-preview", _WikiViewer),
            ("#wiki-placeholder", Static),
        ):
            self._set_display(selector, kind, False)
        title.value = doc.title or ""
        editor.text = doc.content or ""
        self._reset_version_bump_select()
        self._set_authoring_controls(editing=True)
        self._update_draft_tags_display()
        self._update_preview_button()
        self._set_display("#wiki-edit-meta", Horizontal, True)
        self._set_display("#wiki-meta-rule", Rule, True)
        self._set_display("#wiki-editor", TextArea, True)
        editor.focus()  # the body is the usual edit target; title is pre-filled

    def _set_authoring_controls(self, *, editing: bool) -> None:
        """Toggle the authoring controls between new-doc and edit-existing mode:
        new-doc shows the parent picker + "Save draft"; edit shows the
        version-bump picker and hides "Save draft" (it doesn't apply when updating
        an existing document)."""
        self._set_display("#wiki-parent-select", Select, not editing)
        self._set_display("#wiki-version-bump-select", Select, editing)
        self._set_display("#wiki-draft-save-btn", Button, not editing)

    def _exit_authoring(self) -> None:
        """Leave the editor, restoring the previously-open document (or the
        placeholder when none was open)."""
        self._editing = False
        self._edit_target_id = None
        self._editing_draft = None
        self._draft_tags = []
        self._preview = False
        self.refresh_bindings()
        self._set_display("#wiki-edit-meta", Horizontal, False)
        self._set_display("#wiki-editor", TextArea, False)
        self._set_display("#wiki-preview", _WikiViewer, False)
        if self._current_doc_id is not None and self._current_doc is not None:
            self._set_display("#wiki-meta", Horizontal, True)
            self._set_display("#wiki-meta-rule", Rule, True)
            self._set_display("#wiki-viewer", _WikiViewer, True)
        else:
            self._set_display("#wiki-meta-rule", Rule, False)
            self._set_display("#wiki-placeholder", Static, True)

    def _populate_parent_select(self) -> None:
        """Fill the authoring parent picker: always a "New root document" option,
        plus "Child of: <title>" when a document was open as authoring began (the
        viewed doc becomes the new doc's parent). Reset to root each time."""
        try:
            select = self.query_one("#wiki-parent-select", Select)
        except NoMatches:
            return
        options: list[tuple[str, int]] = [("New root document", _ROOT_PARENT)]
        if self._current_doc_id is not None and self._current_doc is not None:
            title = (self._current_doc.title or "Untitled").strip() or "Untitled"
            shown = (
                title
                if len(title) <= _PARENT_TITLE_LIMIT
                else title[: _PARENT_TITLE_LIMIT - 1] + "…"
            )
            options.append((f"Child of: {shown}", self._current_doc_id))
        select.set_options(options)
        select.value = _ROOT_PARENT

    def _selected_parent_id(self) -> int | None:
        """The parent chosen in the authoring picker: a real document id, or None
        for a root document (the _ROOT_PARENT sentinel / no selection)."""
        try:
            select = self.query_one("#wiki-parent-select", Select)
        except NoMatches:
            return None
        value = select.value
        # Select.NULL (not Select.BLANK — that resolves to Widget.BLANK==False and
        # never matches) is the "no selection" sentinel; _ROOT_PARENT means root.
        if value is Select.NULL or value == _ROOT_PARENT:
            return None
        return cast(int, value)

    def _collect_draft(self) -> Draft | None:
        """Build a Draft from the form. Returns None (and surfaces why) when the
        title is missing. Reuses the open draft's path so a save overwrites it."""
        try:
            title_input = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return None
        title = title_input.value.strip()
        if not title:
            self.app.notify("A title is required.", severity="warning")
            title_input.focus()
            return None
        if len(title) > _TITLE_MAX:
            self.app.notify(
                f"Title is too long (max {_TITLE_MAX} characters).",
                severity="warning",
            )
            title_input.focus()
            return None
        body = editor.text
        if len(body) > _CONTENT_MAX:
            self.app.notify(
                f"Content is too long (max {_CONTENT_MAX} characters).",
                severity="warning",
            )
            return None
        path = self._editing_draft.path if self._editing_draft else None
        return Draft(
            title=title,
            tags=list(self._draft_tags),
            body=body,
            path=path,
        )

    async def _publish(self, draft: Draft) -> None:
        """Save the draft to the wiki: create the document, attach tags
        (best-effort, capped at the server max), archive the local file, then
        open the new document."""
        client = self.app.client
        if client is None:
            return
        try:
            save_btn = self.query_one("#wiki-save-btn", Button)
        except NoMatches:
            save_btn = None
        if save_btn is not None:
            save_btn.disabled = True  # block double-submit while in flight
        # Read the chosen parent before _exit_authoring (below) tears the form down.
        parent_id = self._selected_parent_id()
        try:
            try:
                response = await client.wiki.create_document(
                    title=draft.title,
                    content=draft.body,
                    parent_document_id=parent_id,
                )
            except TissueApiError as e:
                log.warning("Wiki: publish failed: %s", e)
                self.app.notify("Couldn't save the document.", severity="error")
                return
            wiki_id = response.id
            if wiki_id is None:
                self.app.notify(
                    "The server didn't return the new document.", severity="error"
                )
                return
            tags = draft.tags[:_MAX_TAGS]
            dropped = len(draft.tags) - len(tags)
            failed: list[str] = []
            for name in tags:
                try:
                    await client.wiki.attach_tag(wiki_id, name=name)
                except TissueApiError as e:
                    log.warning("Wiki: attach tag %r failed: %s", name, e)
                    failed.append(name)
            if draft.path is not None:
                try:
                    self._draft_store().mark_synced(draft.path)
                except OSError as e:
                    log.warning("Wiki: archiving synced draft failed: %s", e)
            self._notify_published(failed, dropped)
        finally:
            if save_btn is not None:
                save_btn.disabled = False
        # Leave the editor, refresh the sidebar, then open the new document.
        self._exit_authoring()
        self._reload_drafts()
        await self._load_tree()
        await self._open_document(wiki_id)

    def _notify_published(self, failed: list[str], dropped: int) -> None:
        notes: list[str] = []
        if dropped:
            notes.append(f"{dropped} extra tag(s) skipped (max {_MAX_TAGS})")
        if failed:
            notes.append(f"couldn't attach: {', '.join(failed)}")
        if notes:
            self.app.notify(f"Document saved. ({'; '.join(notes)})", severity="warning")
        else:
            self.app.notify("Document saved to the wiki.")

    def _reset_version_bump_select(self) -> None:
        try:
            select = self.query_one("#wiki-version-bump-select", Select)
        except NoMatches:
            return
        with select.prevent(Select.Changed):
            select.value = _DEFAULT_VERSION_BUMP

    def _selected_version_bump(self) -> str:
        """The chosen SemanticUpdateType for a content edit (defaults to PATCH)."""
        try:
            value = self.query_one("#wiki-version-bump-select", Select).value
        except NoMatches:
            return _DEFAULT_VERSION_BUMP
        if value is Select.BLANK or value is Select.NULL:
            return _DEFAULT_VERSION_BUMP
        return str(value)

    async def _save_edit(self) -> None:
        """Apply the edit-mode form to the open document: update the title and/or
        content (whichever changed), bumping the version on a content change."""
        client = self.app.client
        doc_id = self._edit_target_id
        if client is None or doc_id is None:
            return
        try:
            title_input = self.query_one("#wiki-edit-title", Input)
            editor = self.query_one("#wiki-editor", TextArea)
        except NoMatches:
            return
        title = title_input.value.strip()
        if not title:
            self.app.notify("A title is required.", severity="warning")
            title_input.focus()
            return
        if len(title) > _TITLE_MAX:
            self.app.notify(
                f"Title is too long (max {_TITLE_MAX} characters).", severity="warning"
            )
            title_input.focus()
            return
        body = editor.text
        if len(body) > _CONTENT_MAX:
            self.app.notify(
                f"Content is too long (max {_CONTENT_MAX} characters).",
                severity="warning",
            )
            return
        title_changed = title != self._edit_original_title.strip()
        body_changed = body != self._edit_original_body
        if not title_changed and not body_changed:
            self.app.notify("No changes to save.")
            self._exit_authoring()
            return
        bump = self._selected_version_bump()
        try:
            save_btn = self.query_one("#wiki-save-btn", Button)
        except NoMatches:
            save_btn = None
        if save_btn is not None:
            save_btn.disabled = True  # block double-submit while in flight
        try:
            # Title first: it doesn't bump the version, so on a both-changed edit
            # the single version bump belongs to the content update.
            if title_changed:
                await client.wiki.update_title(doc_id, title=title)
            if body_changed:
                await client.wiki.update_content(
                    doc_id, content=body, version_update_type=bump
                )
        except TissueApiError as e:
            log.warning("Wiki: edit save failed for %s: %s", doc_id, e)
            self._notify_edit_error(e)
            return
        finally:
            if save_btn is not None:
                save_btn.disabled = False
        self.app.notify("Document updated.")
        # Leave the editor, refresh the tree (the title may have changed), then
        # reopen so the meta header + body reflect the new version/content.
        self._exit_authoring()
        await self._load_tree()
        await self._open_document(doc_id)

    def _notify_edit_error(self, exc: TissueApiError) -> None:
        """Turn a save failure into an actionable message. The two the user can do
        something about are a concurrent edit (the @Version optimistic-lock
        conflict — reopen and retry) and a lock (unlock first).

        The conflict branch comes FIRST and must NOT use a loose "LOCK" substring:
        the backend titles the 409 conflict "OPTIMISTIC_LOCK_FAILED", which itself
        contains "LOCK" — a substring test would mis-route it to the lock message
        (wrong, and unactionable, since there's nothing to unlock). DOCUMENT_LOCKED
        is a 400, so the exact-match lock branch can't be swallowed by the 409 one."""
        title = (exc.title or "").upper()
        if exc.status == 409 or "OPTIMISTIC" in title or "CONFLICT" in title:
            msg = "Someone else changed this document. Reopen it and retry."
        elif title == "DOCUMENT_LOCKED":
            msg = "This document is locked now — unlock it to edit."
        else:
            msg = exc.detail or "Couldn't save your changes."
        self.app.notify(msg, severity="error")

    # The outline is fed by _WikiViewer.sidebar_toc (the viewer forwards
    # TableOfContentsUpdated directly); here we only react to a TOC click.
