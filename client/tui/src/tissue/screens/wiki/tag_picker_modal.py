"""Modal for managing a document's wiki tags: add an existing tag (autocompleted
from the global catalog) or type a new name and pick a colour for it, and remove
already-chosen tags by clicking their chip. Dismisses with the final list of
``(name, colour)`` tuples (colour is a ColorType enum name, or None for "auto"),
or None if cancelled.

The modal only collects the desired set — it never calls attach/detach itself.
The caller diffs against the document's current tags and applies the changes.
"""

from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, Input, Label, Select
from textual_autocomplete import AutoComplete, DropdownItem, TargetState

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.wiki.tag_colors import COLOR_NAMES, tag_chip_style, tag_hex

log = logging.getLogger(__name__)

_MAX_TAGS = 5
# Keep in sync with the backend WikiTagConstraintPolicy.NAME_MAX_LENGTH.
_MAX_TAG_LEN = 18
# Chips wrap to a new row once a row would exceed this width (the dialog's inner
# width — 60 minus padding — with a small safety margin).
_CHIP_ROW_BUDGET = 54
# Sentinel value for the colour Select's "auto" option (server picks a colour).
_AUTO_COLOUR = ""

# (name, colour-enum-or-None)
TagChoice = tuple[str, str | None]


def _dedup(tags: list[TagChoice]) -> list[TagChoice]:
    seen: set[str] = set()
    out: list[TagChoice] = []
    for name, colour in tags:
        cf = name.casefold()
        if cf not in seen:
            seen.add(cf)
            out.append((name, colour))
    return out


def _chip_label(name: str, colour: str | None) -> Text:
    """Chip label: name + remove glyph, drawn on a solid background of the tag's
    own colour (a padded pill); plain text when the tag has no colour."""
    style = tag_chip_style(colour)
    text = f" {name} ✕ "
    return Text(text, style=style) if style else Text(text)


class TagPickerModal(TissueModal[list[TagChoice] | None]):
    CSS_PATH = "tag_picker_modal.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    def __init__(self, initial: list[TagChoice]) -> None:
        super().__init__()
        # Working set of chosen (name, colour), de-duplicated, order preserved.
        self._chosen: list[TagChoice] = _dedup(initial)
        # Catalog: casefold name -> (canonical name, colour) for autocomplete and
        # so re-adding an existing tag shows its real colour.
        self._catalog: dict[str, TagChoice] = {}

    def compose(self) -> ComposeResult:
        with Container(id="tag-picker-dialog", classes="dialog"):
            inp = Input(placeholder="type a tag, Enter to add", id="tag-picker-input")
            yield inp
            yield AutoComplete(inp, candidates=self._candidates)
            with Horizontal(id="tag-picker-add-row"):
                yield Select(
                    self._colour_options(),
                    value=_AUTO_COLOUR,
                    allow_blank=False,
                    id="tag-picker-colour",
                )
                yield Button("Add", id="tag-picker-add", classes="-btn-success")
            with Vertical(id="tag-picker-selected"):
                yield from self._chip_rows()
            yield Label("", id="tag-picker-status", classes="status-msg")
            with Horizontal(id="tag-picker-buttons"):
                yield Button("Cancel", id="tag-picker-cancel", classes="-btn-error")
                yield Button("Done", id="tag-picker-done", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#tag-picker-dialog", Container)
        dialog.border_title = "Tags"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#tag-picker-input", Input).focus()
        self.run_worker(self._load_tags(), exclusive=True, group="tag-catalog")

    @staticmethod
    def _colour_options() -> list[tuple[Text, str]]:
        opts: list[tuple[Text, str]] = [(Text("(auto color)"), _AUTO_COLOUR)]
        for name in COLOR_NAMES:
            label = name.replace("_", " ").title()
            opts.append((Text.assemble(("● ", tag_hex(name)), label), name))
        return opts

    async def _load_tags(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            tags = await client.wiki.search_tags()
        except TissueApiError as e:
            log.debug("Tag picker: couldn't load catalog: %s", e)
            self._status("Couldn't load existing tags — you can still type new ones.")
            return
        self._catalog = {t.name.casefold(): (t.name, t.color) for t in tags if t.name}

    def _candidates(self, state: TargetState) -> list[DropdownItem]:
        # Suggest catalog tags not already chosen; AutoComplete filters by text.
        chosen = {n.casefold() for n, _ in self._chosen}
        return [
            DropdownItem(name)
            for cf, (name, _colour) in self._catalog.items()
            if cf not in chosen
        ]

    def _chip_rows(self) -> list[Widget]:
        """The chosen tags as chip buttons, packed into rows that wrap once a row
        would exceed the dialog width (so long tags flow to the next line)."""
        if not self._chosen:
            return [Label("(no tags yet)", classes="tag-empty")]
        rows: list[list[Button]] = [[]]
        width = 0
        for name, colour in self._chosen:
            chip_w = len(name) + 5  # " name ✕ " (len + 4) + right margin (1)
            if rows[-1] and width + chip_w > _CHIP_ROW_BUDGET:
                rows.append([])
                width = 0
            # Each chip is a button; pressing it removes that tag (name on `name`).
            rows[-1].append(
                Button(_chip_label(name, colour), name=name, classes="tag-chip")
            )
            width += chip_w
        return [Horizontal(*chips, classes="tag-chip-row") for chips in rows]

    async def _refresh_chips(self) -> None:
        try:
            box = self.query_one("#tag-picker-selected", Vertical)
        except NoMatches:
            return
        await box.remove_children()
        await box.mount_all(self._chip_rows())

    @on(Input.Submitted, "#tag-picker-input")
    async def _on_submitted(self, event: Input.Submitted) -> None:
        await self._add(event.value)

    @on(Button.Pressed, "#tag-picker-add")
    async def _on_add_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        await self._add(self.query_one("#tag-picker-input", Input).value)

    async def _add(self, raw: str) -> None:
        name = raw.strip()
        inp = self.query_one("#tag-picker-input", Input)
        if not name:
            return
        if len(name) > _MAX_TAG_LEN:
            self._status(f"A tag is at most {_MAX_TAG_LEN} characters.")
            return
        cf = name.casefold()
        if any(cf == n.casefold() for n, _ in self._chosen):
            inp.value = ""  # already chosen — just clear
            self._status("")
            return
        if len(self._chosen) >= _MAX_TAGS:
            self._status(f"At most {_MAX_TAGS} tags per document.")
            return
        if cf in self._catalog:
            # Existing tag: the server reuses it (and its colour); show that.
            canonical, colour = self._catalog[cf]
            self._chosen.append((canonical, colour))
            self._status(
                f"'{canonical}' already exists — keeping its color.", error=False
            )
        else:
            colour = self._selected_colour()
            self._chosen.append((name, colour))
            self._status("")
        inp.value = ""
        self._reset_colour()
        await self._refresh_chips()
        inp.focus()

    def _selected_colour(self) -> str | None:
        try:
            value = self.query_one("#tag-picker-colour", Select).value
        except NoMatches:
            return None
        if value is Select.BLANK or value == _AUTO_COLOUR:
            return None
        return str(value)

    def _reset_colour(self) -> None:
        try:
            self.query_one("#tag-picker-colour", Select).value = _AUTO_COLOUR
        except NoMatches:
            pass

    @on(Button.Pressed, ".tag-chip")
    async def _on_chip_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        name = event.button.name
        self._chosen = [(n, c) for n, c in self._chosen if n != name]
        self._status("")
        await self._refresh_chips()

    @on(Button.Pressed, "#tag-picker-done")
    def _on_done(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(list(self._chosen))

    @on(Button.Pressed, "#tag-picker-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _status(self, message: str, *, error: bool = True) -> None:
        try:
            label = self.query_one("#tag-picker-status", Label)
        except NoMatches:
            return
        label.remove_class("-error")
        label.update(message)
        if message and error:
            label.add_class("-error")
