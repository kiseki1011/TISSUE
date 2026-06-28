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
    """@-mention autocomplete for a comment `Input`."""

    def __init__(
        self,
        target: Input | str,
        members: Callable[[], list[ProjectMemberSummary]],
    ) -> None:
        super().__init__(target, candidates=None)
        self._members = members

    def _active_mention(self, state: TargetState) -> str | None:
        """Partial text after a word-starting `@`, if the cursor is inside one."""
        before = state.text[: state.cursor_position]
        at_index = before.rfind("@")
        if at_index == -1:
            return None
        # The '@' must start a word (line start or after whitespace), and the text
        # up to the cursor must be a single token with no spaces.
        if at_index > 0 and not before[at_index - 1].isspace():
            return None
        partial = before[at_index + 1 :]
        if any(character.isspace() for character in partial):
            return None
        return partial

    def get_candidates(self, target_state: TargetState) -> list[DropdownItem]:
        partial = self._active_mention(target_state)
        if partial is None:
            return []
        needle = partial.casefold()
        items: list[DropdownItem] = []
        for member in self._members():
            username = member.username
            if not username:
                continue
            name = member.display_name or ""
            if (
                needle
                and needle not in username.casefold()
                and needle not in name.casefold()
            ):
                continue
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
        # Already filtered by username/name in get_candidates. Skip the base's
        # value-only fuzzy match, which would drop name-only matches.
        return candidates

    def get_search_string(self, target_state: TargetState) -> str:
        return self._active_mention(target_state) or ""

    def should_show_dropdown(self, search_string: str) -> bool:
        # Show whenever there are candidates, which only happens inside a mention
        # token (get_candidates returns [] otherwise), including right after '@'.
        return self.option_list.option_count > 0

    def apply_completion(self, value: str, state: TargetState) -> None:
        before = state.text[: state.cursor_position]
        at_index = before.rfind("@")
        if at_index == -1:
            return
        prefix = state.text[:at_index]
        after = state.text[state.cursor_position :]
        # `value` is the item's main text ('@username'), so the '@' isn't duplicated.
        new_value = f"{prefix}{value} {after}"
        new_cursor = len(prefix) + len(value) + 1
        with self.prevent(Input.Changed):
            self.target.value = new_value
            self.target.cursor_position = new_cursor
