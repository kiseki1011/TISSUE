from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


@dataclass(frozen=True)
class MemberFilter:
    """Client-side filter for the [1] Members list (applied to the loaded list).

    - `active`: "all" | "active" | "inactive"
    - `roles`: project roles to keep (empty = any), sorted for stable equality
    """

    active: str = "all"
    roles: tuple[str, ...] = ()

    def __post_init__(self) -> None:
        object.__setattr__(self, "roles", tuple(sorted(self.roles)))

    def matches(self, member: ProjectMemberSummary) -> bool:
        if self.active == "active" and not member.active:
            return False
        if self.active == "inactive" and member.active:
            return False
        return not self.roles or (member.role or "").upper() in self.roles


DEFAULT_MEMBER_FILTER = MemberFilter()
