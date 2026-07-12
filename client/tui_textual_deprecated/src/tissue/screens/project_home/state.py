from __future__ import annotations

from collections.abc import Collection, Mapping
from dataclasses import dataclass, field
from typing import TYPE_CHECKING

from tissue.screens.project_home.issue_filter import DEFAULT_ISSUE_FILTER, IssueFilter
from tissue.screens.project_home.member_filter import (
    DEFAULT_MEMBER_FILTER,
    MemberFilter,
)
from tissue.screens.project_home.sprint_filter import (
    DEFAULT_SPRINT_FILTER,
    SprintFilter,
)

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_detail_view import IssueDetailView
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )
    from tissue.api.generated.models.sprint_summary import SprintSummary


@dataclass
class ProjectHomeFilters:
    issue: IssueFilter = field(default_factory=lambda: DEFAULT_ISSUE_FILTER)
    sprint: SprintFilter = field(default_factory=lambda: DEFAULT_SPRINT_FILTER)
    member: MemberFilter = field(default_factory=lambda: DEFAULT_MEMBER_FILTER)


@dataclass
class IssueListState:
    issues: list[IssueSummary] = field(default_factory=list)
    keyword: str | None = None
    total: int = 0
    page: int = 0
    has_next: bool = False
    loading_more: bool = False


@dataclass
class ProjectMemberListState:
    members: list[ProjectMemberSummary] = field(default_factory=list)
    displayed: list[ProjectMemberSummary] = field(default_factory=list)
    detail_assigned: list[IssueSummary] = field(default_factory=list)
    detail_reviewing: list[IssueSummary] = field(default_factory=list)


@dataclass
class SprintState:
    sprints: list[SprintSummary] = field(default_factory=list)
    detail_id: int | None = None
    detail_issues: list[IssueSummary] = field(default_factory=list)
    detail_status: str | None = None
    edit_current: dict[str, str] = field(default_factory=dict)
    by_id: dict[int, SprintSummary] | None = None


@dataclass
class AgentWorkState:
    issues: list[IssueSummary] = field(default_factory=list)
    names: dict[int, str] = field(default_factory=dict)


@dataclass
class IssueDetailState:
    issue_key: str | None = None
    cache: dict[str, IssueDetailView] = field(default_factory=dict)
    assigned: bool = False
    edit_current: dict[str, str] = field(default_factory=dict)
    custom_fields: dict[int, CustomFieldValueInfo] = field(default_factory=dict)
    field_options: dict[int, list[FieldOptionDetail]] = field(default_factory=dict)
    _fetch_seq: int = 0
    _latest_fetch: dict[str, int] = field(default_factory=dict)

    def begin_fetch(self, issue_key: str) -> int:
        """Claim the newest in-flight detail fetch for `issue_key`.

        Concurrent fetches for the same issue can land out of order. Callers pass the
        returned token to `is_latest_fetch` so only the newest-issued fetch wins.
        """
        self._fetch_seq += 1
        self._latest_fetch[issue_key] = self._fetch_seq
        return self._fetch_seq

    def is_latest_fetch(self, issue_key: str, token: int) -> bool:
        return self._latest_fetch.get(issue_key) == token


@dataclass
class IssueReviewState:
    reviewer_ids: list[int] = field(default_factory=list)
    assignee_id: int | None = None
    is_reviewer: bool = False
    # The issue these reviewer fields describe
    # `v` only acts when this matches the shown issue
    detail_key: str | None = None
    busy: bool = False
    picker_issue_key: str | None = None
    picker_baseline: list[int] = field(default_factory=list)


@dataclass
class IssueHierarchyState:
    hierarchy: str | None = None
    children: list[IssueIdentifierResponse] = field(default_factory=list)
    issue_type_hierarchy: dict[int, str] = field(default_factory=dict)
    busy: bool = False
    picker_issue_key: str | None = None


@dataclass
class IssueRelationState:
    relations: IssueRelationsDetail | None = None
    busy: bool = False
    picker_issue_key: str | None = None


@dataclass
class IssueCommentState:
    reply_to: int | None = None
    reply_targets: dict[int, str] = field(default_factory=dict)
    generation: int = 0


@dataclass
class ProjectHomeUiState:
    expanded: bool = False
    activity_closed: bool = False
    view_mode: str = "issues"

    def restore(
        self,
        saved: Mapping[str, object],
        *,
        valid_view_modes: Collection[str],
    ) -> None:
        self.expanded = bool(saved.get("expanded", self.expanded))
        self.activity_closed = bool(saved.get("activity_closed", self.activity_closed))
        view_mode = saved.get("view_mode")
        if isinstance(view_mode, str) and view_mode in valid_view_modes:
            self.view_mode = view_mode

    def to_config(self) -> dict[str, object | None]:
        return {
            "expanded": self.expanded,
            "activity_closed": self.activity_closed,
            "view_mode": self.view_mode,
        }
