"""Modal for choosing wiki tags: add existing tags (autocompleted from the
global catalog) or type a new name to create one on the fly. Dismisses with the
final list of tag names (≤5), or None if cancelled.

The modal only collects names — it never calls attach/detach itself. The caller
applies them (a draft stores them locally; an open document attaches/detaches).
"""

from __future__ import annotations

import logging

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.css.query import NoMatches
from textual.widgets import Button, Input, Label
from textual_autocomplete import AutoComplete, DropdownItem, TargetState

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

_MAX_TAGS = 5
_MAX_TAG_LEN = 20


class TagPickerModal(TissueModal[list[str] | None]):
    CSS_PATH = "tag_picker_modal.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    def __init__(self, initial: list[str]) -> None:
        super().__init__()
        # Working set of chosen tag names (de-duplicated, order preserved).
        self._selected: list[str] = list(dict.fromkeys(initial))
        # Existing tag names from the catalog, for autocomplete suggestions.
        self._all_names: list[str] = []

    def compose(self) -> ComposeResult:
        with Container(id="tag-picker-dialog", classes="dialog"):
            inp = Input(placeholder="type a tag, Enter to add", id="tag-picker-input")
            yield inp
            yield AutoComplete(inp, candidates=self._candidates)
            with Horizontal(id="tag-picker-selected"):
                yield from self._chip_widgets()
            yield Label("", id="tag-picker-status", classes="status-msg")
            with Horizontal(id="tag-picker-buttons"):
                yield Button("Cancel", id="tag-picker-cancel")
                yield Button("Done", id="tag-picker-done", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#tag-picker-dialog", Container)
        dialog.border_title = "Tags"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#tag-picker-input", Input).focus()
        self.run_worker(self._load_tags(), exclusive=True, group="tag-catalog")

    async def _load_tags(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            tags = await client.wiki.search_tags()
        except TissueApiError as e:
            log.debug("Tag picker: couldn't load catalog: %s", e)
            # Suggestions are unavailable, but typing new tags still works.
            self._status("Couldn't load existing tags — you can still type new ones.")
            return
        self._all_names = [t.name for t in tags if t.name]

    def _candidates(self, state: TargetState) -> list[DropdownItem]:
        # Suggest catalog tags not already chosen; AutoComplete filters by text.
        chosen = {s.casefold() for s in self._selected}
        return [
            DropdownItem(name)
            for name in self._all_names
            if name.casefold() not in chosen
        ]

    def _chip_widgets(self) -> list[Button | Label]:
        if not self._selected:
            return [Label("(no tags yet)", classes="tag-empty")]
        # Each chip is a button; pressing it removes that tag (name carried on
        # the widget's `name`).
        return [
            Button(f"{tag} ✕", name=tag, classes="tag-chip") for tag in self._selected
        ]

    async def _refresh_chips(self) -> None:
        try:
            box = self.query_one("#tag-picker-selected", Horizontal)
        except NoMatches:
            return
        await box.remove_children()
        await box.mount_all(self._chip_widgets())

    @on(Input.Submitted, "#tag-picker-input")
    async def _on_submitted(self, event: Input.Submitted) -> None:
        await self._add(event.value)

    async def _add(self, raw: str) -> None:
        name = raw.strip()
        inp = self.query_one("#tag-picker-input", Input)
        if not name:
            return
        if len(name) > _MAX_TAG_LEN:
            self._status(f"A tag is at most {_MAX_TAG_LEN} characters.")
            return
        if any(name.casefold() == s.casefold() for s in self._selected):
            inp.value = ""  # already chosen — just clear
            self._status("")
            return
        if len(self._selected) >= _MAX_TAGS:
            self._status(f"At most {_MAX_TAGS} tags per document.")
            return
        self._selected.append(name)
        inp.value = ""
        self._status("")
        await self._refresh_chips()
        inp.focus()

    @on(Button.Pressed, ".tag-chip")
    async def _on_chip_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        name = event.button.name
        self._selected = [t for t in self._selected if t != name]
        self._status("")
        await self._refresh_chips()

    @on(Button.Pressed, "#tag-picker-done")
    def _on_done(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(list(self._selected))

    @on(Button.Pressed, "#tag-picker-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _status(self, message: str) -> None:
        try:
            label = self.query_one("#tag-picker-status", Label)
        except NoMatches:
            return
        label.remove_class("-error")
        label.update(message)
        if message:
            label.add_class("-error")
