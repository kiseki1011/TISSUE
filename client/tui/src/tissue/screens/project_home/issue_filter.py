"""The [1] Issues list's filter state, shared between the screen and its modal."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class IssueFilter:
    """The active filter for the project's issue search.

    Frozen + normalised (the category/priority/sprint tuples are sorted in
    `__post_init__`) so two filters with the same selection compare equal regardless
    of the order the modal returned them — that equality is what lights up the ⚙
    button when the filter differs from the default. An empty multi-select field
    means "any" (it's simply omitted from the query).
    """

    # The default narrows to non-terminal issues (matches the sprint open-issue
    # pool's INITIAL/ACTIVE), so a fresh hub hides completed/aborted work.
    state_categories: tuple[str, ...] = ("INITIAL", "ACTIVE")
    priorities: tuple[str, ...] = ()
    # Assignees as the backend's member-id list ("me" and/or "<id>"), OR-matched
    # (issues assigned to ANY listed member); None for anyone.
    assignee_member_ids: tuple[str, ...] | None = None
    # My-review-status filter for the [3] Requested reviews view: narrows it to
    # issues where my review is in one of these statuses (PENDING / APPROVED /
    # CHANGES_REQUESTED). Empty = any status. Only the reviews query uses it.
    reviewer_statuses: tuple[str, ...] = ()
    # Specific sprints, OR-matched. `current_sprint_only` additionally folds the
    # project's active sprint into the query server-side (a union, not a conflict).
    sprint_ids: tuple[int, ...] = ()
    current_sprint_only: bool = False
    # When set, the same state/priority/sprint narrowing also applies to the [3]
    # Agent Work box (its own agent-assignee filter is always kept).
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

    # Typed `search_project_issues` argument accessors (an empty selection → None,
    # i.e. the filter is omitted). Returned per-field rather than as one **kwargs
    # dict so the call sites stay type-checked.
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
