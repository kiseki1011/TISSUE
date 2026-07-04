from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class IssueFilter:
    """The filter now in use for the project's issue search, shared by screen and modal.

    `__post_init__` sorts the tuples so two filters with the same choices compare
    equal no matter what order the modal added them. That equality is what marks
    the ⚙ button when the filter is not the default.
    """

    # Defaults to work still in progress (same as the sprint open list), hiding
    # done/aborted.
    state_categories: tuple[str, ...] = ("INITIAL", "ACTIVE")
    priorities: tuple[str, ...] = ()
    # None means anyone. Otherwise a list of member ids ("me" and/or "<id>") where
    # a match on any one of them passes.
    assignee_member_ids: tuple[str, ...] | None = None
    reviewer_statuses: tuple[str, ...] = ()
    sprint_ids: tuple[int, ...] = ()
    # Added on top of sprint_ids on the server, not a clash with it.
    current_sprint_only: bool = False
    # Also limits the Agent / Reviews lists (their assignee/reviewer filter is kept).
    apply_to_agent: bool = False

    def __post_init__(self) -> None:
        object.__setattr__(
            self, "state_categories", tuple(sorted(self.state_categories))
        )
        object.__setattr__(self, "priorities", tuple(sorted(self.priorities)))
        object.__setattr__(self, "sprint_ids", tuple(sorted(self.sprint_ids)))
        object.__setattr__(
            self, "reviewer_statuses", tuple(sorted(self.reviewer_statuses))
        )
        if self.assignee_member_ids is not None:
            object.__setattr__(
                self, "assignee_member_ids", tuple(sorted(self.assignee_member_ids))
            )

    # One method per field instead of one **kwargs dict, so callers stay type-checked.
    def state_categories_arg(self) -> list[str] | None:
        return list(self.state_categories) or None

    def priorities_arg(self) -> list[str] | None:
        return list(self.priorities) or None

    def assignee_arg(self) -> list[str] | None:
        return list(self.assignee_member_ids) if self.assignee_member_ids else None

    def reviewer_statuses_arg(self) -> list[str] | None:
        return list(self.reviewer_statuses) or None

    def sprint_ids_arg(self) -> list[int] | None:
        return list(self.sprint_ids) or None

    def current_sprint_only_arg(self) -> bool | None:
        return True if self.current_sprint_only else None


DEFAULT_ISSUE_FILTER = IssueFilter()
