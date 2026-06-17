from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.api.generated.models.issue_search_request import IssueSearchRequest
from tissue.api.generated.models.page_issue_summary import PageIssueSummary

if TYPE_CHECKING:
    from tissue.api.client import TissueClient


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
    ) -> PageIssueSummary:
        """Full-text search across every project the caller is a member of."""
        request = IssueSearchRequest(keyword=keyword)
        return await self._client._call_with_retry(
            self._client.issue_api.search_all_issues,
            request,
            page=page,
            size=size,
        )
