"""An @-mention autocomplete for a comment Input: typing '@' opens a dropdown of
project members (matched by username OR display name); selecting one inserts
'@username'. Modelled on textual_autocomplete's PathAutoComplete (which completes
the last path segment), but keyed on the '@' that starts the current word.

The library already does the right thing for Enter: while the dropdown is shown,
Enter accepts the highlighted member and is prevented from submitting the Input;
with the dropdown hidden, Enter submits the comment as usual."""

from __future__ import annotations

from collections.abc import Callable
from typing import TYPE_CHECKING

from textual.widgets import Input
from textual_autocomplete import AutoComplete, DropdownItem, TargetState

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

_MAX_SUGGESTIONS = 30


class MentionAutoComplete(AutoComplete):
    """Completes the `@word` token under the cursor against the project roster.

    `members` is read lazily (a callable) so the dropdown reflects the roster even
    when it loads after the composer is mounted. All matching/insertion is done here
    (the base widget's whole-input fuzzy filter is bypassed) so a member can be found
    by display name even when their username doesn't contain the typed fragment."""

    def __init__(
        self,
        target: Input | str,
        members: Callable[[], list[ProjectMemberSummary]],
    ) -> None:
        super().__init__(target, candidates=None)
        self._members = members

    def _active_mention(self, state: TargetState) -> str | None:
        """The partial username being typed after an '@' that begins the current
        word, or None when the cursor isn't inside a mention token."""
        before = state.text[: state.cursor_position]
        at = before.rfind("@")
        if at == -1:
            return None
        # The '@' must start a word (line start or after whitespace), and the text
        # between it and the cursor must be a single token (no spaces).
        if at > 0 and not before[at - 1].isspace():
            return None
        partial = before[at + 1 :]
        if any(ch.isspace() for ch in partial):
            return None
        return partial

    def get_candidates(self, target_state: TargetState) -> list[DropdownItem]:
        partial = self._active_mention(target_state)
        if partial is None:
            return []
        needle = partial.casefold()
        items: list[DropdownItem] = []
        for m in self._members():
            username = m.username
            if not username:
                continue
            name = m.display_name or ""
            if (
                needle
                and needle not in username.casefold()
                and needle not in name.casefold()
            ):
                continue
            # main is what gets inserted ('@username'); prefix is display-only.
            prefix = f"{name}  " if name and name != username else ""
            items.append(DropdownItem(main=f"@{username}", prefix=prefix))
            if len(items) >= _MAX_SUGGESTIONS:
                break
        return items

    def get_matches(
        self,
        target_state: TargetState,
        candidates: list[DropdownItem],
        search_string: str,
    ) -> list[DropdownItem]:
        # Already filtered by username/name in get_candidates; don't re-filter on the
        # base's value-only fuzzy match (it would drop name-only matches).
        return candidates

    def get_search_string(self, target_state: TargetState) -> str:
        return self._active_mention(target_state) or ""

    def should_show_dropdown(self, search_string: str) -> bool:
        # Show whenever there are candidates — which only happens inside a mention
        # token (get_candidates returns [] otherwise), including right after '@'.
        return self.option_list.option_count > 0

    def apply_completion(self, value: str, state: TargetState) -> None:
        before = state.text[: state.cursor_position]
        at = before.rfind("@")
        if at == -1:
            return
        prefix = state.text[:at]
        after = state.text[state.cursor_position :]
        # `value` is the item's main text ('@username'), so the '@' isn't duplicated.
        new_value = f"{prefix}{value} {after}"
        new_cursor = len(prefix) + len(value) + 1
        with self.prevent(Input.Changed):
            self.target.value = new_value
            self.target.cursor_position = new_cursor
