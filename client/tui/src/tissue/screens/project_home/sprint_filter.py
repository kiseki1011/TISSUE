from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SprintFilter:
    """The filter in use for the project's Sprints list, shared by screen and modal.

    Defaults to the in-progress sprints (Planning/Active), hiding finished ones
    (Completed/Cancelled). `__post_init__` sorts so equality ignores order, which
    is what marks the ⚙ button when the filter is not the default.
    """

    statuses: tuple[str, ...] = ("PLANNING", "ACTIVE")

    def __post_init__(self) -> None:
        object.__setattr__(self, "statuses", tuple(sorted(self.statuses)))

    def statuses_arg(self) -> list[str] | None:
        return list(self.statuses) or None


DEFAULT_SPRINT_FILTER = SprintFilter()
