from __future__ import annotations

import logging
from pathlib import Path

from rich.text import Text
from textual import on
from textual.css.query import NoMatches
from textual.widgets import (
    Label,
    OptionList,
)
from textual.widgets.option_list import Option

from tissue.domain.wiki.drafts import Draft, DraftStore
from tissue.paths import drafts_dir
from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.rendering import (
    _label,
)
from tissue.util.datetime_fmt import format_relative

log = logging.getLogger(__name__)


class DraftsMixin(WikiScreenBase):
    """Lists local drafts and opens one for editing."""

    def _draft_store(self) -> DraftStore:
        """A store rooted at the configured draft folder (or the default)."""
        configured = self.app.config.settings.wiki_draft_dir
        root = Path(configured).expanduser() if configured else drafts_dir()
        return DraftStore(root)

    def _reload_drafts(self) -> None:
        """Repopulate the Local drafts list; the section is hidden when empty."""
        try:
            header = self.query_one("#wiki-drafts-header", Label)
            options = self.query_one("#wiki-drafts", OptionList)
        except NoMatches:
            return
        drafts = self._draft_store().list_drafts()
        options.clear_options()
        for draft in drafts:
            if draft.path is None:
                continue
            options.add_option(Option(self._draft_label(draft), id=str(draft.path)))
        has_drafts = bool(drafts)
        header.display = has_drafts
        options.display = has_drafts

    def _draft_label(self, draft: Draft) -> Text:
        text = Text.assemble("📝 ", _label(draft.title))
        modified = draft.modified_at()
        if modified is not None:
            text.append(f"  {format_relative(modified)}", style="dim")
        return text

    @on(OptionList.OptionSelected, "#wiki-drafts")
    def _on_draft_selected(self, event: OptionList.OptionSelected) -> None:
        if event.option.id is None:
            return
        if self._editing:
            self.app.notify(
                "Save or cancel the current draft first.", severity="warning"
            )
            return
        path = Path(event.option.id)
        try:
            draft = Draft.from_file(path)
        except OSError as e:
            log.warning("Wiki: couldn't open draft %s: %s", path, e)
            self.app.notify("Couldn't open that draft.", severity="error")
            self._reload_drafts()  # it may have been removed/renamed externally
            return
        self._enter_authoring(draft)
