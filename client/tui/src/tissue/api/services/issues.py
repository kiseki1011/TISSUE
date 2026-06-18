from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.page_response_issue_summary import (
    PageResponseIssueSummary,
)

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


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
