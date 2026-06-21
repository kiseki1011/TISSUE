from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.page_response_issue_summary import (
    PageResponseIssueSummary,
)
from tissue.api.generated.models.perform_transition_request import (
    PerformTransitionRequest,
)

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail


# VIBE-CODED
# model: "claude-opus-4-8"
# evaluation: NOT_REVIEWED
class IssueService:
    """Issue read operations."""

    def __init__(self, client: TissueClient) -> None:
        self._client = client

    async def search(
        self,
        *,
        keyword: str | None = None,
        page: int = 0,
        size: int = 20,
    ) -> PageResponseIssueSummary:
        """Full-text search across every project the caller is a member of."""
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            keyword=keyword,
            page=page,
            size=size,
        )

    async def my_work(
        self,
        *,
        page: int = 0,
        size: int = 20,
    ) -> PageResponseIssueSummary:
        """Issues assigned to the current user, across every project they belong to."""
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            assignee_member_ids=["me"],
            page=page,
            size=size,
        )

    async def search_project_issues(
        self,
        project_key: str,
        *,
        keyword: str | None = None,
        page: int = 0,
        size: int = 50,
    ) -> PageResponseIssueSummary:
        """Issues within a single project (keyword search), for the project hub."""
        return await self._client._call_with_retry(
            self._client.issue_api.search_project_issues,
            project_key=project_key,
            keyword=keyword,
            page=page,
            size=size,
        )

    async def get_issue(self, issue_key: str) -> IssueCommonDetail:
        """The common (read) fields of a single issue."""
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_common,
            issue_key=issue_key,
        )

    async def get_transitions(self, issue_key: str) -> list[AvailableTransition]:
        """Workflow transitions available from the issue's current state."""
        return await self._client._call_with_retry(
            self._client.issue_api.get_issue_available_transitions,
            issue_key=issue_key,
        )

    async def perform_transition(self, issue_key: str, transition_id: int) -> None:
        """Execute a workflow transition, moving the issue to a new state."""
        await self._client._call_with_retry(
            self._client.issue_api.perform_issue_transition,
            issue_key=issue_key,
            perform_transition_request=PerformTransitionRequest(
                transitionId=transition_id
            ),
        )

    async def assign_issue(self, issue_key: str, member_id: int) -> None:
        """Assign the issue to a project member."""
        await self._client._call_with_retry(
            self._client.issue_api.assign_issue,
            issue_key=issue_key,
            member_id=member_id,
        )

    async def unassign_issue(self, issue_key: str) -> None:
        """Clear the issue's assignee."""
        await self._client._call_with_retry(
            self._client.issue_api.unassign_issue,
            issue_key=issue_key,
        )
